# Guia de Implementação — Correções Backend (Sprint 1 e 2)

**Origem:** bugs-producao-railway-2026-06-05.md + ADR-011  
**Data:** 2026-06-05  
**Status:** Backlog — aguardando implementação

---

## Visão Geral

Todas as correções foram analisadas com contexto do código real. Nenhuma suposição — cada mudança tem causa raiz confirmada no código-fonte.

---

## SPRINT 1 — RBAC e Regras de Negócio (alta prioridade)

### Bloco 1A — Novo escopo INTAKE (ADR-011)

**Impacto:** resolve bugs #2, #6, #10, #17 e o comportamento correto de pipeline por setor

---

#### Passo 1: `PermissionScope.java`

Adicionar `INTAKE` ao enum:

```java
public enum PermissionScope {
    GLOBAL,
    SECTOR,
    OWN,
    INTAKE,
}
```

---

#### Passo 2: `PermissionService.java`

**2a.** Adicionar helper privado e caso `INTAKE` em `resolveScope()`:

```java
private boolean isIntakeSector(Sector sector) {
    return sector == Sector.LEADS || sector == Sector.ATTENDANT;
}
```

Em `resolveScope()`, adicionar após o caso `OWN`:

```java
case INTAKE -> isIntakeSector(user.getSector()) && isIntakeSector(targetSector);
```

**2b.** Adicionar método `getScope()` para uso nos services de search (Fase 3):

```java
public Optional<PermissionScope> getScope(User user, Resource resource, Action action) {
    Optional<PermissionRule> ruleOpt =
            ruleRepository.findByRoleAndSectorAndResourceAndAction(
                    user.getRole(), user.getSector(), resource, action);

    if (ruleOpt.isEmpty()) {
        ruleOpt = ruleRepository.findByRoleAndResourceAndAction(
                user.getRole(), resource, action);
    }

    return ruleOpt.filter(PermissionRule::isAllowed)
                  .map(PermissionRule::getScope);
}
```

---

#### Passo 3: `PermissionSeeder.java`

Substituir as regras afetadas. A tabela abaixo mostra exatamente o que muda:

**USER_LEADS** — apenas CUSTOMER:READ muda:
```java
// antes:
rules.add(rule(USER_LEADS, LEADS, CUSTOMER, READ, OWN));
// depois:
rules.add(rule(USER_LEADS, LEADS, CUSTOMER, READ, INTAKE));
```

**USER_ATTENDANT** — três regras mudam:
```java
// antes:
rules.add(rule(USER_ATTENDANT, ATTENDANT, CUSTOMER, READ, OWN));
rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, READ, OWN));
rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, UPDATE, OWN));
// depois:
rules.add(rule(USER_ATTENDANT, ATTENDANT, CUSTOMER, READ, INTAKE));
rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, READ, INTAKE));
rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, UPDATE, INTAKE));
```

**ADM_LEADS** — três regras mudam:
```java
// antes:
rules.add(rule(ADM_LEADS, LEADS, CUSTOMER, READ, SECTOR));
rules.add(rule(ADM_LEADS, LEADS, TICKET, READ, SECTOR));
rules.add(rule(ADM_LEADS, LEADS, TICKET, UPDATE, SECTOR));
// depois:
rules.add(rule(ADM_LEADS, LEADS, CUSTOMER, READ, INTAKE));
rules.add(rule(ADM_LEADS, LEADS, TICKET, READ, INTAKE));
rules.add(rule(ADM_LEADS, LEADS, TICKET, UPDATE, INTAKE));
```

---

### Bloco 1B — CUSTOMER:READ para setor comercial (bugs #2, #6)

**Causa confirmada:** `ADM_COMMERCIAL` e `USER_COMMERCIAL` não têm nenhuma regra para `CUSTOMER` no seeder. `CustomerServiceImpl.findById()` chama `checkOrThrow(CUSTOMER, READ, ...)` → regra não encontrada → 403.

Adicionar ao bloco `ADM_COMMERCIAL` e `USER_COMMERCIAL` no seeder:

```java
// ADM_COMMERCIAL
rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, CUSTOMER, READ, SECTOR));

// USER_COMMERCIAL
rules.add(rule(USER_COMMERCIAL, COMMERCIAL, CUSTOMER, READ, SECTOR));
```

---

### Bloco 1C — DealServiceImpl: applyDiscount usa Action errada (bug #14)

**Causa confirmada:** `DealServiceImpl.applyDiscount()` linha 174 usa `Action.CONFIGURE`. Nenhuma role comercial tem `DEAL:CONFIGURE` no seeder. O seeder concede `DEAL:UPDATE`.

Alterar linha 174 de `DealServiceImpl.java`:

```java
// antes:
permissionService.checkOrThrow(user, DEAL, CONFIGURE, user.getSector(), deal.getCreatedBy());
// depois:
permissionService.checkOrThrow(user, DEAL, UPDATE, user.getSector(), deal.getCreatedBy());
```

---

### Bloco 1D — DealServiceImpl: closeDeal scope OWN falha para USER_COMMERCIAL (bug #15)

**Causa confirmada:** `closeDeal()` chama `checkOrThrow` com `deal.getCreatedBy()` como `targetOwnerId`. O deal foi criado pelo avaliador. `USER_COMMERCIAL` tem `DEAL:CLOSE (OWN)` → `commercial.id ≠ evaluator.id` → `AccessDeniedException`.

`ADM_COMMERCIAL` tem `DEAL:CLOSE (SECTOR)` e passa, mas `USER_COMMERCIAL` falha.

Alterar `PermissionSeeder.java` — `USER_COMMERCIAL` `DEAL:CLOSE`:
```java
// antes:
rules.add(rule(USER_COMMERCIAL, COMMERCIAL, DEAL, CLOSE, OWN));
// depois:
rules.add(rule(USER_COMMERCIAL, COMMERCIAL, DEAL, CLOSE, SECTOR));
```

**Verificação adicional obrigatória antes de implementar:**  
Investigar se `DealHistoryServiceImpl.record()` causa 500 ao serializar `LocalDateTime` via `tools.jackson.databind.ObjectMapper` sem `JavaTimeModule`. Se o projeto não tiver `JavaTimeModule` registrado, `writeValueAsString(LocalDateTime)` lança `JacksonException` → cai no handler genérico → 500. Verificar se existe uma classe `@Configuration` que configura o `ObjectMapper`.

---

### Bloco 1E — Migração de dados no banco de produção (Railway)

⚠️ **Obrigatório após deploy do novo seeder.**

O `PermissionSeeder` tem early return: `if (permissionRuleRepository.count() > 0) return;`. Com regras existentes no banco, o seeder não roda novamente automaticamente.

**Solução para Railway:** executar via console do banco (ou migration SQL) antes ou imediatamente após o deploy:

```sql
DELETE FROM permission_rules;
```

O seeder rodará no próximo startup e recriará todas as regras com os novos escopos.

> Alternativa de longo prazo: trocar o early return por lógica de upsert no seeder (não é obrigatório agora).

---

## SPRINT 2 — Business Logic, Analytics e Segurança

### Bloco 2A — AnalyticsServiceImpl: scope OWN com null (bug #16)

**Causa confirmada:** todos os métodos chamam `checkOrThrow(user, ANALYTICS, READ, null, null)`. `USER_ATTENDANT` tem `ANALYTICS:READ (OWN)`. `resolveScope(OWN)`: `user.getId().equals(null)` = false → 403.

**Alteração em `AnalyticsServiceImpl.getUserPerformance()`:**

```java
// antes:
permissionService.checkOrThrow(currentUser, ANALYTICS, Action.READ, null, null);
// depois:
permissionService.checkOrThrow(currentUser, ANALYTICS, Action.READ,
        currentUser.getSector(), targetUserId);
```

Com isso: scope OWN → `currentUser.getId().equals(targetUserId)` → atendente só consulta a própria performance. GLOBAL (ADM_SYSTEM) sempre passa.

Os demais endpoints de analytics (`getAdsRoi`, `getConversionByStage`, `getDropOffBySector`, `getGlobalDashBoard`) **não devem ser alterados** — o 403 para `USER_ATTENDANT` nesses endpoints é correto, pois são métricas globais da clínica.

---

### Bloco 2B — UserServiceImpl: validar senha atual no changePassword (bug #5)

**Causa confirmada:** `UserServiceImpl.updatePassword(username, newPassword)` não verifica a senha atual. `PasswordEncoder` já está injetado na classe.

**Alterar método em `UserServiceImpl.java`:**

```java
// assinatura atual:
public UserResponseDTO updatePassword(String username, String newpassword)

// nova assinatura:
public UserResponseDTO updatePassword(String username, String currentPassword, String newPassword)
```

**Lógica a adicionar antes de `user.setPasswordHash()`:**

```java
if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
    throw new IllegalStateException("Senha atual incorreta");
}
```

`IllegalStateException` já está mapeado no `GlobalExceptionHandler` → retorna 422. Nenhuma alteração no handler necessária.

**Também atualizar:**
- DTO de request: adicionar campo `currentPassword`
- `AuthController` ou `UserController`: atualizar a chamada para passar `currentPassword`

**Caso especial — reset por ADM_SYSTEM (regra de negócio):**

ADM_SYSTEM deve poder redefinir a senha de qualquer usuário sem precisar informar a senha atual (ex: recuperação de acesso sem excluir o usuário).

Implementar como **endpoint separado**, não no mesmo método:
- Endpoint existente: `PATCH /users/{username}/password` — exige `currentPassword` + `newPassword` (self-service)
- Novo endpoint: `PATCH /users/{id}/reset-password` — requer role `ADM_SYSTEM`, recebe apenas `newPassword`

O RBAC do novo endpoint deve verificar `checkOrThrow(user, USER, UPDATE, null, targetUser.getId())` para garantir que apenas ADM_SYSTEM (GLOBAL) pode executar.

---

### Bloco 2C — Pipeline filter scope-aware (Fase 3 RBAC — bugs #1, #11 parcial)

**Contexto:** `LeadTicketServiceImpl.search()` e `CustomerServiceImpl.search()` retornam todos os registros após o check de RBAC. O filtro por setor no SQL é o que define o que cada role vê.

**Dependências que precisam ser criadas:**

`LeadTicketRepository.java` — adicionar queries:
```java
Page<LeadTicket> findByCurrentSector(Sector sector, Pageable pageable);
Page<LeadTicket> findByCurrentSectorIn(List<Sector> sectors, Pageable pageable);
Page<LeadTicket> findByCreatedBy(UUID createdBy, Pageable pageable);
```

**Lógica de filtro em `LeadTicketServiceImpl.search()`:**

Após o check de RBAC, substituir a chamada de `findAll()` por filtro baseado no escopo do usuário:

```java
PermissionScope scope = permissionService
        .getScope(user, TICKET, READ)
        .orElseThrow(() -> new AccessDeniedException("Access denied"));

// aplica filtro de pipeline antes dos filtros de parametro da query
Page<LeadTicket> base = switch (scope) {
    case GLOBAL -> ticketRepository.findAll(pageable);
    case SECTOR -> ticketRepository.findByCurrentSector(user.getSector(), pageable);
    case INTAKE -> ticketRepository.findByCurrentSectorIn(
                        List.of(Sector.LEADS, Sector.ATTENDANT), pageable);
    case OWN    -> ticketRepository.findByCreatedBy(user.getId(), pageable);
};
```

> **Nota:** os filtros de parâmetros da query (`customerId`, `status`, `userId`) precisam ser reaproveitados dentro do escopo base. A refatoração completa do search() faz parte deste bloco.

**Para `CustomerServiceImpl.search()`:** requer campo `createdBySector: Sector` na entidade `Customer` (similar a `Deal.createdBySector`) para filtrar no SQL por setor do criador. Avaliar como task separada se aumentar o escopo do sprint.

---

## Ordem de Implementação Recomendada

```
Sprint 1 (todos no mesmo PR, impacto mínimo de risco):
  1. PermissionScope.java          → adicionar INTAKE
  2. PermissionService.java        → resolveScope INTAKE + getScope()
  3. PermissionSeeder.java         → todas as regras deste guia
  4. DealServiceImpl.java          → CONFIGURE → UPDATE + DEAL:CLOSE scope
  [DEPLOY] → limpar tabela permission_rules no Railway

Sprint 2 (PRs separados por subsistema):
  5. AnalyticsServiceImpl.java     → checkOrThrow getUserPerformance
  6. UserServiceImpl.java + DTOs   → validar senha atual + reset ADM_SYSTEM
  7. LeadTicketRepository.java     → novas queries de filtro
  8. LeadTicketServiceImpl.java    → search() scope-aware
```

---

## O que NÃO muda neste plano

- `TRANSITION_ROLES` em `LeadTicketServiceImpl` — continua como guarda de transições por role
- `ALLOWED_TRANSITIONS` — sem alteração
- Lógica de avaliação (USER_EVALUATOR/ADM_EVALUATOR) — permissões já estão corretas no seeder
- `ContactLogServiceImpl` — sem alteração neste sprint
- `GlobalExceptionHandler` — sem alteração (AccessDeniedException já mapeado para 403)

---

## Referências Cruzadas

- `ADR-011-intake-scope-cross-sector-acesso.md` — decisão arquitetural do escopo INTAKE
- `ADR-004-rbac-padrao-checkorThrow-funnel.md` — padrão de chamada ao checkOrThrow (Padrão 3 agora tem filtro SQL definido)
- `bugs-producao-railway-2026-06-05.md` — lista original dos 17 bugs
- `PermissionSeeder.java` — matriz RBAC completa (fonte de verdade)
- `PermissionService.java` — implementação de resolveScope e canAccess