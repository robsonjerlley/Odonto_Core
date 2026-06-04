# Security Gaps — Módulo funnel: ausência de RBAC nos services

**Data:** 2026-05-28  
**Resolvido em:** 2026-06-04  
**Status:** ✅ TODOS OS GAPS CRÍTICOS E IMPORTANTES RESOLVIDOS (Fase 3 aberta — ver Definition of Done)  
**Origem:** revisão cruzada entre PermissionSeeder e services do módulo funnel  
**Impacto:** CustomerServiceImpl, LeadTicketServiceImpl, ContactLogServiceImpl  
**Decisões tomadas:** ADR-003 (ContactLog imutável), ADR-004 (padrão checkOrThrow)  

---

## Modelo de segurança do projeto

O projeto tem três camadas de proteção:

| Camada | Mecanismo | O que garante |
|---|---|---|
| 1 | `JwtAuthFilter` (Spring Security) | Bloqueia qualquer request sem token JWT válido |
| 2 | `SecurityUtils.getCurrentUser()` | Resolve o `User` do SecurityContext; lança `SecurityException` se não autenticado |
| 3 | `PermissionService.checkOrThrow()` | Aplica RBAC: busca `PermissionRule` (role+sector+resource+action) e resolve scope (GLOBAL/SECTOR/OWN) |

A camada 1 garante que nenhum endpoint é acessível sem autenticação. As camadas 2 e 3 são responsabilidade do service. **O gap identificado é a ausência da camada 3 — RBAC não está sendo aplicado na maioria das operações.**

---

## Gap 1 — CustomerServiceImpl

`PermissionService` não está injetado. Autenticação (camada 2) presente apenas em `create` via `getCurrentUser()`. RBAC (camada 3) ausente em todos os métodos.

| Método | Camada 2 (`getCurrentUser`) | Camada 3 (`checkOrThrow`) | Regra no seeder | Status |
|---|:---:|:---:|---|:---:|
| `create` | ✅ | ❌ | CREATE: leads/attendant (OWN/SECTOR) | **GAP RBAC** |
| `update` | ❌* | ❌ | UPDATE: leads/attendant (OWN/SECTOR) | **GAP RBAC** |
| `search` | ❌* | ❌ | READ: leads/attendant (OWN/SECTOR) | **GAP RBAC** |
| `findById` | ❌* | ❌ | READ: leads/attendant (OWN/SECTOR) | **GAP RBAC** |
| `findByCpf` | ❌* | ❌ | READ: leads/attendant (OWN/SECTOR) | **GAP RBAC** |
| `deleteById` | ❌* | ❌ | DELETE: apenas ADM_SYSTEM (GLOBAL) | **CRÍTICO** |

\* Autenticação garantida pela camada 1 (JwtAuthFilter), mas o `User` não é resolvido no método — impossível aplicar scope OWN/SECTOR sem ele.

**Risco crítico:** `deleteById` — a regra do seeder restringe DELETE a ADM_SYSTEM, mas sem `checkOrThrow` qualquer perfil autenticado pode deletar um Customer.

---

## Gap 2 — LeadTicketServiceImpl

`PermissionService` não está injetado. Camada 2 presente em `create` e `changeStatus` via `getCurrentUserId()`. RBAC ausente em todos os métodos.

| Método | Camada 2 (`getCurrentUser`) | Camada 3 (`checkOrThrow`) | Regra no seeder | Status |
|---|:---:|:---:|---|:---:|
| `create` | ✅ | ❌ | CREATE: apenas leads (OWN/SECTOR) — attendant proibido | **CRÍTICO** |
| `changeStatus` | ✅ | ❌ | UPDATE: múltiplos perfis; LOSS/IN_CONTACT proibidos para attendant | **CRÍTICO** |
| `findById` | ❌* | ❌ | READ: todos os perfis com scope variado | **GAP RBAC** |
| `search` | ❌* | ❌ | READ: todos os perfis com scope variado | **GAP RBAC** |
| `deleteById` | ❌* | ❌ | DELETE: apenas ADM_SYSTEM (GLOBAL) | **CRÍTICO** |

\* Autenticação garantida pela camada 1.

**Riscos críticos:**

- `create` — USER_ATTENDANT não deve criar ticket manualmente (US-FUND-02: HTTP 403). Atualmente não é bloqueado.
- `changeStatus` — USER_ATTENDANT não deve transitar para LOSS ou IN_CONTACT. A única validação atual é CPF para SCHEDULED. Bloqueio por perfil não existe.
- `deleteById` — DELETE restrito a ADM_SYSTEM no seeder; sem `checkOrThrow`, qualquer autenticado deleta.

---

## Gap 3 — ContactLogServiceImpl

`PermissionService` injetado. `create` protegido nas duas camadas. `findById`, `search` e `delete` sem RBAC.

| Método | Camada 2 (`getCurrentUser`) | Camada 3 (`checkOrThrow`) | Regra no seeder | Status |
|---|:---:|:---:|---|:---:|
| `create` | ✅ | ✅ | CREATE: leads/attendant (OWN/SECTOR) | ✅ OK |
| `findById` | ❌* | ❌ | READ: todos os perfis com scope variado | **GAP RBAC** |
| `search` | ❌* | ❌ | READ: todos os perfis com scope variado | **GAP RBAC** |
| `delete` | ❌* | ❌ | **Nenhuma regra** — DELETE não concedido a nenhum perfil | **CRÍTICO** |

\* Autenticação garantida pela camada 1.

**Risco crítico:** `delete` — o seeder não concede DELETE a nenhum perfil para CONTACT_LOG. O endpoint existe e não tem proteção RBAC. Qualquer autenticado pode apagar registros de auditoria.

---

## Resumo de criticidade

| Gap | Service | Operação | Criticidade |
|---|---|---|:---:|
| DELETE sem regra no seeder e sem RBAC | ContactLogServiceImpl | `delete` | CRÍTICO |
| USER_ATTENDANT pode criar ticket | LeadTicketServiceImpl | `create` | CRÍTICO |
| USER_ATTENDANT pode transitar para LOSS/IN_CONTACT | LeadTicketServiceImpl | `changeStatus` | CRÍTICO |
| Qualquer autenticado pode deletar Ticket | LeadTicketServiceImpl | `deleteById` | CRÍTICO |
| Qualquer autenticado pode deletar Customer | CustomerServiceImpl | `deleteById` | CRÍTICO |
| READ sem scope enforcement (3 services) | Customer, Ticket, ContactLog | `findById`, `search`, `findByCpf` | MÉDIO |
| CREATE/UPDATE sem RBAC (Customer) | CustomerServiceImpl | `create`, `update` | MÉDIO |

---

## Definition of Done

> **Status geral: CONCLUÍDO** — Todos os gaps críticos e importantes foram resolvidos (verificado em 2026-06-04).

**Fase 1 — Críticos:**
- [x] `ContactLogController` / `ContactLogService` / `ContactLogServiceImpl` — `delete()` e `@DeleteMapping("/{id}")` removidos *(ADR-003)*
- [x] `ContactLogServiceImpl.create()` — `targetOwnerId` corrigido para `user.getId()`; `statusBefore`/`statusAfter` corrigidos para `null` em logs manuais
- [x] `LeadTicketServiceImpl` — `PermissionService` injetado; `checkOrThrow` em `create` e `changeStatus`; validação explícita de USER_ATTENDANT para LOSS/IN_CONTACT
- [x] `CustomerServiceImpl` — `PermissionService` injetado; `checkOrThrow` em `anonymize` (deleteById)
- [x] `LeadTicketServiceImpl` — sem `deleteById` exposto (endpoint não existe)

**Fase 2 — Importantes:**
- [x] `CustomerServiceImpl` — `checkOrThrow` em `create`, `update`, `findById`, `findByCpf`, `search`
- [x] `ContactLogServiceImpl` — `checkOrThrow` em `findById` e `search`
- [x] `LeadTicketServiceImpl` — `checkOrThrow` em `findById` e `search`

**Fase 3 — Débito técnico futuro (aberto):**
- [ ] Scope-aware query filtering: `search()` deve filtrar no SQL por `createdBy`/`currentSector` conforme o scope do perfil

**Testes:**
- [ ] Testes de integração: HTTP 403 para USER_ATTENDANT em `POST /tickets` e `PATCH /tickets/{id}/status` com LOSS e IN_CONTACT

---

## Referências Cruzadas

- `ADR-003-contactlog-imutabilidade-delete-proibido.md` — decisão de remover o endpoint DELETE
- `ADR-004-rbac-padrao-checkorThrow-funnel.md` — padrão correto de chamada ao `checkOrThrow`
- `PermissionSeeder.java` — fonte de verdade da matriz RBAC
- `PermissionService.java` — `checkOrThrow()` lança `AccessDeniedException` (HTTP 403)
- `SecurityUtils.java` — `getCurrentUser()` resolve o `User` e garante contexto autenticado
- `us-fundacional.md` — US-FUND-02 define a matriz de permissões do USER_ATTENDANT