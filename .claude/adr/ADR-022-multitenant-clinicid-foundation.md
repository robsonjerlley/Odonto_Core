# ADR-022: Multi-tenancy Foundation — clinicId em User + JWT

**Status**: Aceito — fundação implementada. **Enforcement substituído pela ADR-024**
**Data**: 2026-06-17 (revisado 2026-06-22)
**Autores**: Arquiteto-Agent
**Impacto**: `User`, `JwtService`, `JwtAuthFilter`, `UserPrincipal`
**Relaciona**: ADR-005 (JWT strategy), ADR-023 (TicketWonEvent), ADR-024 (enforcement `@TenantId`), ADR-025 (RLS futura), spec-redis-cache.md

---

> ## ⚠️ Revisão 2026-06-22 — enforcement movido para ADR-024
>
> A **fundação** desta ADR está implementada e válida: `clinicId` em `User`, claim no JWT
> (`JwtUtil.generateToken` + `extractClinicId`), e `MainUser.getClinicId()`.
>
> A **estratégia de enforcement** descrita nas seções "5" e "Regra mandatória para código novo"
> abaixo — filtrar manualmente por `findByIdAndClinicId(...)` e setar `entity.setClinicId(...)`
> em cada service — foi considerada frágil (depende de disciplina humana; um `findById` esquecido
> = vazamento entre clínicas). Ela foi **substituída** pelo enforcement arquitetural via
> Hibernate `@TenantId` + `TenantContext` na **[ADR-024](ADR-024-tenant-isolation-enforcement-tenantid.md)**.
>
> A camada de defesa em profundidade no banco (RLS PostgreSQL) está documentada como evolução
> futura na **[ADR-025](ADR-025-rls-postgresql-defense-in-depth.md)**.
>
> As seções marcadas com 🔄 abaixo permanecem como registro histórico do raciocínio original.

---

## Contexto

O sistema opera hoje como single-tenant implícito: não há `clinicId` em nenhuma entidade. Toda query retorna dados sem isolamento por clínica — aceitável para MVP, inaceitável para escala multi-clínica.

Dois problemas convergem e forçam esta decisão agora:

1. **Módulos Financeiro e Consultas** (baseados em `TicketStatus.WIN`) estão prestes a ser implementados. Entidades criadas sem `clinicId` precisarão de retrofit garantido após a migração multi-tenant — custo evitável se a fundação for estabelecida antes.

2. **Cache Redis** (spec-redis-cache.md): chaves sem `clinicId` tornam a invalidação de uma clínica global — evicta o cache de todas as clínicas em qualquer escrita. Inaceitável em produção multi-tenant.

Esta ADR define o **escopo mínimo** da fundação multi-tenant: o suficiente para que código novo nasça correto, sem bloquear o desenvolvimento dos módulos enquanto a migração das entidades legadas de `crm_db` é executada separadamente.

**O que NÃO está nesta ADR**: migração das entidades existentes em `crm_db` (`Customer`, `LeadTicket`, `ContactLog`, `Deal`, `DealHistory`, `AdsInvestment`, `RecycleConfig`, `BonusConfig`). Esse trabalho é isolado e não bloqueia os módulos novos.

---

## Decisão

### 1. `clinicId UUID NOT NULL` adicionado a `User` (`identity_db`)

```java
// User.java
@Column(name = "clinic_id", nullable = false)
private UUID clinicId;
```

Flyway migration:

```sql
-- V[n]__add_clinic_id_to_user.sql
ALTER TABLE users ADD COLUMN clinic_id UUID;
-- popular com valor real antes de adicionar NOT NULL
UPDATE users SET clinic_id = gen_random_uuid() WHERE clinic_id IS NULL;
ALTER TABLE users ALTER COLUMN clinic_id SET NOT NULL;
```

`User` é a fonte de verdade do `clinicId`. Todas as outras entidades derivam o valor do usuário autenticado no momento da escrita — sem entidade `Clinic` separada neste momento.

---

### 2. `clinicId` como claim JWT

`JwtService.generateToken()` inclui `clinicId` no payload:

```java
.claim("clinicId", user.getClinicId().toString())
```

Novo método de extração em `JwtService`:

```java
public UUID extractClinicId(String token) {
    return UUID.fromString(
        extractClaim(token, claims -> claims.get("clinicId", String.class))
    );
}
```

O `clinicId` viaja no token — sem round-trip adicional ao banco por request.

---

### 3. `UserPrincipal` carrega `clinicId`

```java
// UserPrincipal.java
private final UUID clinicId;

public static UserPrincipal from(User user) {
    return new UserPrincipal(
        user.getId(), user.getEmail(), user.getPasswordHash(),
        user.getRole(), user.getSector(),
        user.getClinicId()   // ← novo
    );
}

public UUID getClinicId() { return clinicId; }
```

---

### 4. `JwtAuthFilter` extrai e propaga `clinicId`

```java
// JwtAuthFilter.java — dentro do bloco de validação de token
UUID clinicId = jwtService.extractClinicId(token);
// UserPrincipal já carrega clinicId via from(user) — nenhuma alteração no filter
// se o UserPrincipal for construído a partir do token (não do banco), extrair explicitamente:
// UserPrincipal.withClinicId(principal, clinicId)
```

---

### 5. `PermissionService` não muda nesta ADR  🔄 *(o filtro de tenant por query foi substituído pela ADR-024)*

O `checkOrThrow()` mantém a assinatura atual. O filtro de tenant (`clinicId`) é ortogonal ao scope (GLOBAL/SECTOR/OWN): RBAC resolve *o que* o usuário pode fazer; o filtro de tenant resolve *de qual clínica* são os dados. São camadas independentes.

O filtro de tenant é aplicado diretamente nas queries dos services, não no RBAC.

---

## Regra mandatória para código novo  🔄 *SUBSTITUÍDA pela ADR-024*

> 🔄 **Esta seção foi substituída.** Com o `@TenantId` da ADR-024, o Hibernate preenche o
> `clinicId` no INSERT e injeta o `WHERE clinic_id = ?` em toda query automaticamente. Os
> `setClinicId(...)` e `findByIdAndClinicId(...)` manuais descritos abaixo **devem ser removidos**
> dos services — não escreva código novo seguindo este padrão. Mantido como registro histórico.

Todo service que **escreve** em `crm_db` deve propagar `clinicId` do `UserPrincipal`:

```java
entity.setClinicId(currentUser.getClinicId());
```

Todo service que **lê** de `crm_db` deve filtrar por `clinicId`:

```java
repository.findByIdAndClinicId(id, currentUser.getClinicId())
    .orElseThrow(() -> new ResourceNotFoundException(...));
```

Entidade escrita sem `clinicId` = bug de vazamento de dados entre clínicas.

---

## Consequências positivas

- Código novo (módulos Financeiro e Consultas) nasce correto — zero retrofit após ADR-022 completo.
- Cache Redis herda `clinicId` nas chaves automaticamente, tornando a invalidação cirúrgica por clínica.
- Migração do legado (`crm_db`) pode ser executada de forma incremental e isolada, sem bloquear novos módulos.
- `clinicId` no JWT elimina round-trip ao banco para resolver o tenant por request.
- Tokens existentes (sem `clinicId`) são invalidados naturalmente no re-login após o deploy.

## Consequências negativas / riscos

| Risco | Mitigação |
|---|---|
| Tokens existentes sem `clinicId` invalidam requisições após deploy | Forçar re-login: revogar refresh tokens no deploy via reset de `tokenVersion` (se implementado) ou expiração natural |
| `UPDATE users SET clinic_id = gen_random_uuid()` popula usuários da mesma clínica com IDs distintos | Rodar migration com valor fixo por clínica em staging antes de produção |
| Entidades legadas de `crm_db` sem `clinicId` retornam dados misturados enquanto migração não ocorre | Aceitável no contexto single-tenant atual; risco zero enquanto há uma única clínica em produção |

---

## Alternativas descartadas

- **Schema por tenant** (cada clínica com schema PostgreSQL próprio): isolamento máximo, mas migrations duplicadas e operação complexa no Railway. Descartada por custo operacional incompatível com o porte atual.
- **Entidade `Clinic` separada com FK**: mais expressivo no domínio, mas adiciona JOIN em toda query. Postergado: `clinicId` como UUID em `User` resolve o isolamento sem overhead; `Clinic` pode ser criada quando o produto exigir dados da própria clínica (nome, CNPJ, configurações visuais).
- **Filtro de tenant via `@PostFilter` Spring Security**: mistura autorização com isolamento de dados. Descartado por violar separação de responsabilidades.

---

## Ordem de implementação

```
1. V[n]__add_clinic_id_to_user.sql  → Flyway migration
2. User.java                         → campo clinicId
3. JwtService.java                   → claim "clinicId" + extractClinicId()
4. UserPrincipal.java                → campo clinicId + from()
5. JwtAuthFilter.java                → extrair clinicId (se UserPrincipal não for do banco)
6. [código novo a partir daqui]      → usar currentUser.getClinicId() em todo write/read
```

---

## Referências

- ADR-005 — JWT single-token strategy (claims existentes: id, email, role, sector — clinicId é adicionado)
- ADR-023 — TicketWonEvent contract (primeiro consumidor de `clinicId` em eventos de domínio)
- spec-redis-cache.md — chaves de cache para código novo devem incluir `clinicId` após esta ADR
