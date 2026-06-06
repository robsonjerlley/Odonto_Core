# ADR-013: JPA Specifications para listagens scope-aware com filtros cumulativos

**Status**: Aceito
**Data**: 2026-06-06
**Autores**: Arquiteto-Agent
**Impacto**: `LeadTicketRepository`, `CustomerRepository`, `ContactLogRepository`, `UserRepository`,
`*ServiceImpl.search()` do módulo funnel + identity, novas classes `*Specifications`
**Substitui**: parte de listagem/filtro da ADR-012 (Padrão B)

---

## Contexto

A Fase 3 do RBAC (ADR-011/012) tornou o **scope sempre aplicado** em `search()`, somado a
**filtros de query opcionais** (ex.: `customerId`, `status`, `assignedTo` em tickets). Duas dimensões
de recorte passaram a coexistir no mesmo request: **autorização (scope)** e **negócio (filtro)**.

O código entregue na Fase 3 (`LeadTicketServiceImpl.search()`) resolve apenas o scope via `switch` e
**descarta os filtros de query** — viola o contrato §7.4 (regressão M1 da avaliação 2026-06-06). As
saídas alternativas testadas são todas ruins:

| Tentativa | Queries/response | Problema |
|-----------|------------------|----------|
| Método derivado por combinação | 1, mas explode | produto cartesiano scope × filtro de métodos no repository |
| Query do scope + filtrar em memória | 1 | quebra paginação (`totalElements` errado, páginas furadas) |
| `checkOrThrow` + query scope + query filtro | 2+ | volta ao que a ADR-012 quis evitar |

A premissa que a ADR-012 usou para **rejeitar** JPA Specifications era:

> "os filtros de busca em `search()` são mutuamente exclusivos (apenas um entra por request)."

Essa premissa **caiu**. Na Fase 3 scope e filtro são **cumulativos** (`scope AND filtro`). A regra
derivada da própria ADR-012 ("usar Specification se filtros forem compostos E cumulativos E a matriz
crescer") passou a ter as duas primeiras condições satisfeitas.

> **Nota sobre "uma query":** um `Page<T>` no Spring Data sempre executa **2 SQLs** — um `SELECT ... LIMIT/OFFSET`
> e um `SELECT count(*)`. Isso é inerente à paginação. A meta desta ADR é **1 SELECT de dados + 1 COUNT,
> com scope e filtros resolvidos no banco** — não "1 query absoluta", que é impossível com `Page`.

---

## Decisão

### 1. Adotar JPA Specifications como padrão de listagem dinâmica do projeto

Repositórios com listagem scope-aware passam a estender `JpaSpecificationExecutor<T>`. O `search()`
compõe predicados:

```
Specification<T> spec = Specification
        .where(scopeSpec)          // predicado de autorização (vem do PermissionScope)
        .and(filtro1)              // cada filtro só entra se o param != null
        .and(filtro2);
repository.findAll(spec, pageable); // 1 SELECT + 1 COUNT
```

Aplica-se a: **LeadTicket, Customer, ContactLog, User** (`search()`/listagens paginadas).

### 2. `getScope()` permanece a fonte do escopo

Sem alteração em `PermissionService`. O `switch` por scope **deixa de escolher método de repositório**
e passa a **produzir um fragmento `Specification`**. Continua uma única consulta de scope por request.

### 3. Filtros passam a ser CUMULATIVOS (AND)

Mudança semântica em relação à ADR-001/contrato §7.3/§7.4/§7.5: filtros deixam de ser
"mutuamente exclusivos por prioridade" e passam a combinar via `AND`. `?status=SCHEDULED&assignedTo=X`
retorna tickets `SCHEDULED` **E** atribuídos a X.

### 4. Customer e ContactLog: SECTOR/INTAKE via subquery `EXISTS` no LeadTicket

A ADR-012 decidiu que Customer/ContactLog cairiam em `findAll` para SECTOR/INTAKE (por não terem
coluna de setor). **Esta ADR substitui essa decisão**: o setor é propriedade do `LeadTicket`, e
Customer/LeadTicket/ContactLog estão todos no `crm_db` — logo um predicado `EXISTS` é viável e correto.

| Escopo | LeadTicket | Customer | ContactLog |
|--------|-----------|----------|-----------|
| GLOBAL | sem predicado | sem predicado | sem predicado |
| OWN | `createdBy = :userId` | `createdBy = :userId` | `userId = :userId` |
| SECTOR | `currentSector = :sector` | `EXISTS(ticket WHERE ticket.customerId = customer.id AND ticket.currentSector = :sector)` | `EXISTS(ticket WHERE ticket.id = log.ticketId AND ticket.currentSector = :sector)` |
| INTAKE | `currentSector IN (:intake)` | `EXISTS(... ticket.currentSector IN (:intake))` | `EXISTS(... ticket.currentSector IN (:intake))` |

**Por que `EXISTS` e não denormalizar `currentSector` no Customer:** um cliente tem **vários**
LeadTickets ao longo do tempo (reciclagem cria tickets-filho, `previousTicketId` encadeia). "O setor
do cliente" é ambíguo — uma coluna denormalizada exigiria sincronização e introduziria bug de estado.
O `EXISTS` ("o cliente tem ALGUM ticket em setor permitido") é a interpretação semanticamente correta
e não precisa de migração de schema.

**Custo:** uma subquery correlacionada por request. Desprezível no volume do MVP (milhares de linhas).
Reavaliar se o volume crescer ordens de grandeza.

---

## Camadas e reuso (Clean Architecture)

```
[Controller]  params crus + Pageable
     ▼
[Service]     getScope() ─► ScopeSpec        (autorização)
              params     ─► FilterSpec*       (negócio, opcionais)
              where(scope).and(filtros)
     ▼
[Repository : JpaSpecificationExecutor<T>]   findAll(spec, pageable)
     ▲
[*Specifications]  fragmentos estáticos reusáveis e unit-testáveis
```

- **Single Responsibility:** scope não conhece filtro; filtro não conhece scope; service apenas compõe.
- **Reuso:** fragmentos (`createdBy`, `currentSectorIn`, `hasStatus`, ...) reaproveitam em qualquer
  endpoint futuro.
- **Limite consciente:** `Specification` é JPA Criteria — vive na camada de persistência (junto do
  repository). Em monólito em camadas (não-hexagonal) este é o lugar correto. **Não** criar abstração
  genérica de "filtro de domínio" agora — seria over-engineering.

---

## Migração de banco

**Nenhuma alteração de DDL é necessária** — Specifications é só construção de query.

⚠️ **Pré-condição operacional (já existente):** o projeto usa `spring.jpa.hibernate.ddl-auto=update`
e **não tem Flyway/Liquibase**. Mudanças de RBAC que dependem do `PermissionSeeder` exigem reset de
dados em produção, pois o seeder tem early-return:

```sql
-- executar no banco de produção (Railway) antes de validar mudanças de matriz RBAC
DELETE FROM permission_rules;
```

> Ver ADR-014 (proposta) para adoção de ferramenta de migração — `ddl-auto=update` em produção é
> débito técnico de fundação (sem revisão de alterações de schema, sem rollback, acúmulo de drift).

---

## Consequências Positivas

- **1 SELECT + 1 COUNT** por request, scope e filtros resolvidos no banco.
- Resolve **M1** (filtros ignorados em tickets) e **M2** (OWN/SECTOR não aplicados em Customer/ContactLog → vazamento entre donos/setores).
- Remove código morto: 4 métodos privados órfãos em `LeadTicketServiceImpl` e a dependência órfã `userRepository` (**L1**).
- Predicados reutilizáveis e testáveis isoladamente.
- **Contrato HTTP inalterado** (mesmos endpoints, params e `Page<T>`).

## Consequências Negativas / Riscos

- **Mudança semântica de contrato:** filtros exclusivos-por-prioridade → cumulativos-AND. Exige
  atualização do `frontend-integration-contract.md` (§7.3/§7.4/§7.5 + §15) e varredura no frontend
  para telas que enviem múltiplos params esperando prioridade.
- `EXISTS` adiciona uma subquery por request em Customer/ContactLog (negligenciável no MVP).
- Specifications acoplam-se à persistência (aceitável no monólito em camadas).

## Alternativas Consideradas

- **QueryDSL** — descartado: adiciona dependência + annotation processor sem ganho no volume atual.
- **`@Query` com `:param IS NULL OR campo = :param`** — descartado: frágil com enums, sem reuso (já rejeitado na ADR-012).
- **Hibernate `@Filter`/`@FilterDef`** — descartado: scope é dinâmico por usuário (OWN usa id, INTAKE usa lista); esconde a autorização da vista, anti-pattern de auditabilidade num domínio de rastreabilidade.
- **Postgres Row-Level Security** — descartado no MVP: acopla ao Postgres e conflita com o modelo dual-datasource/cross-db por UUID. Reavaliar se virar multi-clínica com isolamento duro.
- **Denormalizar `currentSector` no Customer** — descartado: cliente tem múltiplos tickets em setores distintos → coluna ambígua + bug de sincronização.

---

## Referências Cruzadas

- `ADR-011` — escopo INTAKE e `getScope()`
- `ADR-012` — Padrão B (list) substituído por esta ADR; rejeição de Specifications revista (premissa de exclusividade caiu)
- `ADR-001` — semântica de filtros de query (prioridade → cumulativo AND)
- `avaliacao-backend-2026-06-06.md` — M1, M2, L1
- `frontend-integration-contract.md` — §7.3, §7.4, §7.5, §15 (atualizados)