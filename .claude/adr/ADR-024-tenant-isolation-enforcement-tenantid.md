# ADR-024: Tenant Isolation Enforcement — `@TenantId` + `TenantContext`

**Status**: Implementado
**Data**: 2026-06-22
**Autores**: Arquiteto-Agent
**Impacto**: todas as entidades multi-tenant, `JwtAuthenticationFilter`, listeners da ADR-023, jobs `@Scheduled`, `AnalyticsServiceImpl`, Flyway
**Relaciona**: ADR-022 (fundação clinicId — **refina o enforcement**), ADR-023 (TicketWonEvent), ADR-020 (Virtual Threads), ADR-005 (JWT), spec-redis-cache.md
**Substitui**: a estratégia de filtro manual por query definida na ADR-022 ("Regra mandatória para código novo")

> 🎯 **Nota (2026-06-28):** as menções a "listeners da ADR-023" / "módulos Financeiro/Consultas" abaixo
> referem-se ao `TicketWonEvent` async, hoje [substituído pela ADR-029](ADR-029-scheduling-agenda-evaluator-deal-snapshot.md).
> O consumidor real do fechamento (`AppointmentEventListener`, ADR-029) é **síncrono**, então **não**
> precisa do `TenantContext.set/clear` do §6 — o tenant do request já está no contexto. A regra do §6
> continua **obrigatória** para qualquer `@Async`/`@Scheduled` futuro; só não se aplica ao `appointment`.

---

## Contexto

A ADR-022 estabeleceu a fundação multi-tenant: `clinicId` em `User`, claim no JWT, `MainUser.getClinicId()` e `JwtUtil.extractClinicId()` — **tudo já implementado**.

O que a ADR-022 deixou como *convenção* foi o enforcement:

```java
// ADR-022 — dependia de o dev lembrar disto em TODA query
repository.findByIdAndClinicId(id, currentUser.getClinicId());
entity.setClinicId(currentUser.getClinicId());
```

⚠️ **Problema estrutural**: isolamento de tenant baseado em disciplina humana não é isolamento. Qualquer `findById()` que escape ao invés de `findByIdAndClinicId()` é um vazamento de dados entre clínicas — e o compilador não acusa. Com os módulos Financeiro e Consultas (ADR-023) prestes a multiplicar o número de queries, o risco cresce linearmente com o código.

Esta ADR substitui a convenção por **enforcement arquitetural**: o ORM passa a injetar o filtro de tenant automaticamente, e o desenvolvedor não consegue mais escrever uma query sem isolamento por esquecimento.

---

## Decisão

Adotar **multi-tenancy por discriminator nativo do Hibernate 6+/7 (`@TenantId`)**, alimentado por um `TenantContext` próprio (não pelo `SecurityContext`).

### Análise de Trade-offs

| Critério | Filtro manual (ADR-022) | `@TenantId` (esta ADR) | RLS PostgreSQL (ADR-025) | Peso |
|---|---|---|---|---|
| Enforcement | ❌ Convenção | ✅ ORM | ✅✅ Banco | Alto |
| Risco de data leak | 🚫 Alto | ✅ Baixo | ✅ Mínimo | Alto |
| Complexidade de implementação | Média | ✅ Baixa | ⚠️ Média-alta | Alto |
| Mudança em services existentes | ❌ Todas as queries | ✅ Remove boilerplate | ✅ Zero | Médio |
| Atrito com HikariCP/Railway | ✅ Nenhum | ✅ Nenhum | ⚠️ Sensível | Médio |
| Dependências extras | Zero | Zero | Zero | Médio |

🎯 **`@TenantId` é a fundação correta agora**: enforcement na camada de aplicação com custo baixo e zero atrito operacional. A RLS (ADR-025) fica como evolução de defesa em profundidade na camada do banco — não exclui esta decisão, complementa.

---

### 1. Campo `@TenantId` nas entidades multi-tenant

```java
import org.hibernate.annotations.TenantId;

@Entity
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId                       // ← Hibernate assume o controle do campo
    @Column(name = "clinic_id", nullable = false, updatable = false)
    private UUID clinicId;
    // ...
}
```

A partir daqui, para toda entidade com `@TenantId`, o Hibernate:

- **preenche `clinic_id` no INSERT** com o tenant do `TenantContext` — `entity.setClinicId(...)` deixa de ser necessário;
- **adiciona `AND clinic_id = ?`** em `findById`, `findAll`, JPQL e `JpaSpecificationExecutor` automaticamente.

> ⚠️ **Limitação conhecida**: queries `@Query(nativeQuery = true)` **NÃO são filtradas** pelo `@TenantId`. Hoje não há nenhuma nos repositories; qualquer query nativa futura deve adicionar `WHERE clinic_id = :clinicId` manualmente e ser sinalizada em code review.

### 2. `TenantContext` — fonte única de verdade do tenant (ThreadLocal)

**Novo arquivo**: `config/tenant/TenantContext.java`

```java
public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    public static void set(UUID clinicId) { CURRENT.set(clinicId); }
    public static UUID get()              { return CURRENT.get(); }
    public static void clear()            { CURRENT.remove(); }

    private TenantContext() {}
}
```

O `TenantContext` é populado por **duas fontes**, nunca lido diretamente do `SecurityContext` pelo resolver:

```
        ┌──────────────────────────────┐
        │  TenantContext (ThreadLocal)  │ ← única fonte do resolver
        └──────────────────────────────┘
              ▲                    ▲
   request HTTP                listener / job
   (JwtAuthenticationFilter)   (set a partir do payload do evento
    set(mainUser.getClinicId)   ou iteração por clínica)
```

> 📐 **Por que NÃO ler o `SecurityContext` direto**: o `SecurityContextHolder` usa `ThreadLocal` e **não propaga para threads `@Async`** (listeners da ADR-023) nem existe em jobs `@Scheduled`. Acoplar o resolver ao `SecurityContext` faria os módulos Financeiro/Consultas nascerem com `clinicId` nulo. Ver ADR-023 (revisão 2026-06-22).

### 3. `ClinicTenantResolver` — ponte para o Hibernate

**Novo arquivo**: `config/tenant/ClinicTenantResolver.java`

```java
@Component
public class ClinicTenantResolver implements CurrentTenantIdentifierResolver<UUID> {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.get();   // pode ser null fora de request/listener — ver §6
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
```

### 4. Wiring do resolver no Hibernate

**Novo arquivo**: `config/tenant/HibernateTenantConfig.java`

```java
@Configuration
public class HibernateTenantConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantResolverCustomizer(ClinicTenantResolver resolver) {
        return props -> props.put(
            AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
```

> Nota técnica: no Hibernate 6+/7 o `@TenantId` ativa o discriminator multi-tenancy por entidade automaticamente. **Não** é necessário `hibernate.multiTenancy=DISCRIMINATOR` (legado dos modos antigos) nem um `MultiTenantConnectionProvider` (esse é exclusivo dos modos DATABASE/SCHEMA). Basta fornecer o `CurrentTenantIdentifierResolver`.

### 5. `JwtAuthenticationFilter` popula o `TenantContext` no request

O filtro já reconstrói o `MainUser` (que carrega `clinicId`). Passa a setar o `TenantContext` e a limpá-lo ao fim da cadeia:

```java
// dentro de doFilterInternal, após autenticar:
TenantContext.set(mainUser.getClinicId());
try {
    filterChain.doFilter(request, response);
} finally {
    TenantContext.clear();   // ThreadLocal não pode vazar para a próxima request do pool
}
```

> O `clinicId` também está disponível via `JwtUtil.extractClinicId(token)` sem ida ao banco — útil se o filtro for otimizado para não recarregar o usuário (ver dívida em "Consequências").

### 6. Comportamento fora de request — regra mandatória

Quando `TenantContext.get()` retorna `null` (job `@Scheduled`, `@Async` sem set, startup), **nenhuma operação sobre entidade multi-tenant é permitida**. Cada contexto sem request deve estabelecer o tenant explicitamente:

- **Listeners `@Async` (ADR-023)**: `TenantContext.set(event.clinicId())` + `try/finally clear()`.
- **Jobs cross-tenant** (ex.: reconciliação da ADR-023): iterar as clínicas e, para cada uma, `set(clinicId)` → query → `clear()`. Operações intencionalmente cross-tenant (relatório consolidado de admin) usam SQL nativo auditado — ver limitação no §1.

---

## Arquivos atingidos

### Novos

| Arquivo | Responsabilidade |
|---|---|
| `config/tenant/TenantContext.java` | ThreadLocal com o `clinicId` atual; fonte única do resolver |
| `config/tenant/ClinicTenantResolver.java` | `CurrentTenantIdentifierResolver<UUID>` lendo o `TenantContext` |
| `config/tenant/HibernateTenantConfig.java` | `HibernatePropertiesCustomizer` que registra o resolver |

### Modificados

| Arquivo | Mudança |
|---|---|
| `config/security/JwtAuthenticationFilter.java` | `set/clear` do `TenantContext` no request |
| `modules/funnel/domain/model/Customer.java` | `@TenantId` no `clinicId` (após migration) |
| `modules/funnel/domain/model/LeadTicket.java` | idem |
| `modules/funnel/domain/model/ContactLog.java` | idem |
| `modules/commercial/domain/model/Deal.java` | idem |
| `modules/commercial/domain/model/DealHistory.java` | idem |
| `modules/commercial/domain/model/AdsInvestment.java` | idem |
| `modules/commercial/domain/model/RecycleConfig.java` | idem (ver nota config global) |
| `modules/commercial/domain/model/BonusConfig.java` | idem (ver nota config global) |
| Services de `funnel` e `commercial` | **remover** `setClinicId(...)` e `findByIdAndClinicId(...)` — agora automático |
| Entidades dos módulos Financeiro/Consultas (ADR-023) | nascem com `@TenantId` — ver ADR-023 revisada |

> ⚠️ **`RecycleConfig` / `BonusConfig`**: hoje são tratados como config global (`key = 'recycle-config'` na spec-redis). Decidir por entidade se são **por clínica** (recebem `@TenantId`) ou **globais ao sistema** (NÃO recebem `@TenantId` e ficam fora do isolamento). Recomendação: por clínica, já que cada clínica terá sua própria regra de reciclagem/bônus. Confirmar com o PO.

### Não modificados (e por quê)

| Arquivo | Motivo |
|---|---|
| `User.java` | `clinicId` é a **fonte** do tenant, não um campo filtrado — NÃO recebe `@TenantId` |
| `PermissionRule` | RBAC é ortogonal ao tenant (ADR-022 §5); regras de permissão são globais por role/sector |
| `JwtUtil.java` | Já implementado (claim + `extractClinicId`) |
| `MainUser.java` | Já carrega `clinicId` |

---

## Migração das entidades legadas (`crm_db`)

As 8 entidades de `crm_db` precisam ganhar a coluna `clinic_id` **antes** de receber `@TenantId` (campo `NOT NULL` não pode ser anotado sobre tabela sem a coluna populada).

### Estratégia incremental (uma migration por entidade ou em lote)

```sql
-- V[n]__add_clinic_id_to_crm_entities.sql
-- Executar por tabela: customers, lead_tickets, contact_logs, deals,
--                       deal_history, ads_investment, recycle_config, bonus_config

ALTER TABLE customers ADD COLUMN clinic_id UUID;

-- Popular com a clínica única atual (single-tenant em produção hoje).
-- ⚠️ usar o MESMO clinicId já atribuído ao User em produção (NÃO gen_random_uuid por linha):
UPDATE customers SET clinic_id = :clinica_unica_em_producao WHERE clinic_id IS NULL;

ALTER TABLE customers ALTER COLUMN clinic_id SET NOT NULL;
CREATE INDEX idx_customers_clinic_id ON customers (clinic_id);  -- toda query filtra por ele
```

| Passo | Detalhe |
|---|---|
| 1. Descobrir o `clinicId` de produção | É o valor populado em `tb_users` pela migration da ADR-022. Capturar antes de migrar `crm_db` |
| 2. Adicionar coluna nullable | Permite deploy sem downtime |
| 3. Popular com o `clinicId` único | Valor fixo — todas as linhas da clínica atual recebem o mesmo ID |
| 4. `SET NOT NULL` | Após popular |
| 5. Índice em `clinic_id` | Obrigatório: passa a participar de toda query |
| 6. Anotar `@TenantId` na entidade | Só depois da coluna `NOT NULL` populada |

> ⚠️ **Ordem crítica**: a anotação `@TenantId` e o deploy do código devem vir **depois** da migration que popula a coluna. Anotar antes = `INSERT` tenta gravar tenant em coluna inexistente.

> ✅ **Risco zero hoje**: há uma única clínica em produção. A migração mistura dados de zero clínicas — todas as linhas pertencem à mesma. O isolamento real só passa a importar quando a segunda clínica entrar.

---

## Ordem de implementação

```
1. config/tenant/TenantContext.java            → ThreadLocal
2. config/tenant/ClinicTenantResolver.java      → resolver lendo TenantContext
3. config/tenant/HibernateTenantConfig.java     → registrar resolver no Hibernate
4. JwtAuthenticationFilter.java                 → set/clear no request (try/finally)
5. Migration Flyway por entidade de crm_db      → add coluna + popular + NOT NULL + índice
6. @TenantId nas entidades migradas             → após cada migration correspondente
7. Limpar services                              → remover setClinicId/findByIdAndClinicId manuais
8. [ADR-023] listeners + entidades novas         → já nascem com @TenantId + TenantContext.set
9. Teste de isolamento                          → IT com 2 clínicas (ver abaixo)
```

---

## Teste de isolamento (obrigatório)

Testcontainers (já no `pom.xml`) com **duas clínicas** populadas:

```
@Test  — clínica A não enxerga Customer da clínica B via findById
@Test  — findAll retorna apenas registros da clínica do TenantContext
@Test  — save() sem TenantContext (null) falha de forma explícita, não grava null
@Test  — listener com TenantContext.set(event.clinicId()) grava na clínica correta
@Test  — após o request, TenantContext.get() == null (clear funcionou)
```

---

## Consequências positivas

- Enforcement deixa de depender de disciplina: o ORM injeta o filtro. `findById` volta a ser seguro.
- A "regra mandatória" da ADR-022 e da ADR-023 §4 (`setClinicId` em cada save) **deixa de existir** — vira comportamento do Hibernate.
- O ponto de atenção humana migra de *N queries* para *poucos pontos de `TenantContext.set`* (filtro de request + listeners) — auditável.
- Zero dependência nova; compatível com Spring Boot 4.0.5 + Hibernate 7 já no projeto.
- Caminho aberto para a RLS (ADR-025) como camada adicional, sem retrabalho.

## Consequências negativas / riscos

| Risco | Mitigação |
|---|---|
| Query `nativeQuery=true` escapa do `@TenantId` | Proibir/auditar em code review; adicionar `WHERE clinic_id` manual quando inevitável |
| `TenantContext` não limpo vaza tenant para próxima task do pool | `try/finally clear()` obrigatório em filtro, listeners e jobs; teste de isolamento cobre |
| `@Async`/`@Scheduled` sem `set` → resolver null | Regra do §6: nenhuma operação multi-tenant sem tenant estabelecido; falha explícita |
| Operações cross-tenant (admin, reconciliação) ficam mais trabalhosas | Iterar por clínica ou SQL nativo auditado — limitação inerente a qualquer discriminator |
| Cache Redis **não** é coberto pelo `@TenantId` | `clinicId` explícito na chave continua obrigatório — ver spec-redis-cache.md |
| `JwtAuthenticationFilter` ainda recarrega usuário do banco | Dívida herdada da ADR-022; o `clinicId` no token já permite otimizar para zero round-trip — fora do escopo desta ADR |

---

## Alternativas descartadas

- **Manter filtro manual (ADR-022 original)**: descartado — não é enforcement, é convenção; risco linear ao volume de código.
- **Hibernate `@Filter`/`@FilterDef`**: funciona, mas exige ativar o filtro por sessão e não preenche o `clinicId` no INSERT automaticamente. Mais boilerplate que `@TenantId` sem ganho.
- **RLS PostgreSQL agora**: superior em segurança, mas adiciona atrito com HikariCP/Railway e gestão de role não-owner — custo incompatível com o porte atual. Documentada como evolução em **ADR-025**.

---

## Referências

- ADR-022 — fundação clinicId em User + JWT (esta ADR substitui a estratégia de enforcement manual)
- ADR-023 — TicketWonEvent (revisão 2026-06-22: listeners com `TenantContext.set`)
- ADR-025 — RLS PostgreSQL (defesa em profundidade futura)
- ADR-020 — Virtual Threads (interação com `@Async` e ThreadLocal)
- spec-redis-cache.md — `@TenantId` não cobre o cache; chave com `clinicId` é obrigatória
- [Hibernate ORM — `@TenantId` discriminator multi-tenancy](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#multitenacy)
