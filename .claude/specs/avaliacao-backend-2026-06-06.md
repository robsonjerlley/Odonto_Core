# Avaliação de Backend — OdontoCore CRM

**Data:** 2026-06-06
**Origem:** revisão completa do backend contra CLAUDE.md, contrato de integração, ADR-001..012 e specs de produção
**Escopo:** identity, funnel, commercial, analytics + security/exception
**Fonte da verdade:** código Java em `src/main/java`
**Status:** Backlog — diagnóstico, aguardando implementação (correções não escritas neste doc)

---

## Veredito executivo

O guia `impl-backend-corrections-sprint1.md` lista 8 correções (Sprint 1 + 2). Na conferência contra o código:

- **Implementado:** apenas o **Bloco 1A** (escopo `INTAKE`, ADR-011).
- **Não implementado:** os outros 7 itens (1B, 1C, 1D, 2A, 2B, 2C parcial, #11/ADM_CLINICAL).
- **Regressão introduzida:** a Fase 3 (`search()` scope-aware) passou a ignorar os filtros de query e deixou métodos órfãos.

Além do backlog, foram encontradas **3 falhas críticas que não constam em nenhuma spec** (C2, C4, C5 abaixo).

> **Já corrigido (fora deste doc):** typo `findByCreateBy` → `findByCreatedBy` em `LeadTicketRepository`, que impedia o boot. Resolvido pelo autor antes desta documentação.

---

## 🔴 CRÍTICO

### C2. Módulo `Users` sem nenhum enforcement de RBAC

**Arquivos:** `UserController.java`, `UserServiceImpl.java`

Nenhum método (`create`, `delete`, `search`, `findById`, `updatePassword`) chama
`permissionService.checkOrThrow`. Com qualquer token válido (inclusive `USER_ATTENDANT`):

- `POST /users` cria usuário **com qualquer role, inclusive `ADM_SYSTEM`** → escalonamento de privilégio.
- `DELETE /users/{id}` apaga qualquer usuário.
- `PATCH /users/{username}/newPassword` redefine senha de qualquer usuário (ver C3).

O contrato §8 declara `USER:CREATE = ADM_SYSTEM apenas` — não enforçado em lugar algum.

**Direção:** aplicar `checkOrThrow(user, USER, <action>, ...)` nos métodos do service, coerente
com o padrão dos demais módulos. A matriz `USER:*` (GLOBAL para ADM_SYSTEM) já existe no seeder.

---

### C3. Troca de senha — bug #5 não corrigido + IDOR

**Arquivo:** `UserServiceImpl.java:73` (`updatePassword`), `UserController.java:38`

- `oldPassword` existe no DTO (`UserPasswordUpdateRequestDTO.java:10`) mas **nunca é lido** →
  bug #5 (validação de senha atual) continua aberto.
- O `username` vem no path e não há verificação de que é o próprio usuário autenticado →
  **IDOR**: qualquer um troca a senha de qualquer um.

**Direção (conforme Bloco 2B do guia):** validar `passwordEncoder.matches(oldPassword, hash)` antes
de gravar; endpoint separado de reset para ADM_SYSTEM. Garantir que o `username` alvo == usuário
do token (ou ADM_SYSTEM).

---

### C4. `DealHistory` viola NOT NULL em toda gravação → 500

**Arquivos:** `DealHistory.java:23-27`, `DealHistoryServiceImpl.java:30-35`

`changedBy` e `changedBySector` são `@Column(nullable = false)`, mas `record()` recebe `User user`
por parâmetro e **nunca seta esses campos**. Todo `record()` insere linha com `changedBy=null` →
`DataIntegrityViolationException` → handler genérico → **HTTP 500**.

Afeta `DealServiceImpl.update()`, `applyDiscount()` e `closeDeal()`.

> **Esta é a verdadeira causa-raiz do bug #15** (500 ao fechar deal). A suspeita do guia (linha 158)
> sobre `ObjectMapper`/`LocalDateTime` está descartada: `writeValueAsString(null)` devolve `"null"`
> sem estourar. A divergência #3 do contrato (changedBy null) subestimava — não é "campo nulo no
> response", é violação de constraint que derruba a operação.

**Direção:** `record()` deve preencher `changedBy = user.getId()` e `changedBySector = user.getSector()`
a partir do `User` que já recebe.

---

### C5. `RecycleJob` cria ticket-filho que viola NOT NULL

**Arquivo:** `RecycleJob.java:83-87`

O novo `LeadTicket` é criado sem `currentSector` nem `createdBy`, ambos `@Column(nullable = false)`
(`LeadTicket.java:33,39`) → `DataIntegrityViolationException` no `save` → o job de reciclo **falha**.

O contrato §11 trata como "LACUNA: campos ficam null"; na prática é pior — o registro não persiste.

**Direção:** definir `currentSector` (provável `LEADS` — reinício da esteira) e `createdBy` (UUID de
sistema ou herdado do ticket original) no ticket-filho.

---

## 🟠 ALTO — Correções declaradas no guia mas NÃO aplicadas

### A1. Bugs #2/#6 — `CUSTOMER:READ` comercial (Bloco 1B)
`PermissionSeeder.java`: não há regra `CUSTOMER:READ` para `ADM_COMMERCIAL` nem `USER_COMMERCIAL`.
`CustomerServiceImpl.findById()` → `checkOrThrow(CUSTOMER, READ, ...)` → 403. Nome do paciente some
para o comercial. (`USER_EVALUATOR`=GLOBAL e `ADM_EVALUATOR`=SECTOR já existem; só o comercial ficou de fora.)

### A2. Bug #14 — `applyDiscount` ainda usa `CONFIGURE` (Bloco 1C)
`DealServiceImpl.java:176`: `checkOrThrow(user, DEAL, CONFIGURE, ...)`. Nenhuma role comercial tem
`DEAL:CONFIGURE` no seeder → 403 ao aplicar desconto. Era trocar para `UPDATE`.

### A3. Bug #15 (parte scope) — `USER_COMMERCIAL DEAL:CLOSE` ainda `OWN` (Bloco 1D)
`PermissionSeeder.java:160`. O deal é criado pelo avaliador; `closeDeal` passa `deal.getCreatedBy()`
como owner (`DealServiceImpl.java:222`). Para `USER_COMMERCIAL` (OWN): `commercial.id ≠ evaluator.id`
→ 403. (Combinado com C4, vira 500 antes mesmo de chegar aqui.)

### A4. Bug #16 — Analytics scope `OWN` (Bloco 2A)
`AnalyticsServiceImpl` — todos os métodos chamam `checkOrThrow(currentUser, ANALYTICS, READ, null, null)`
(ex.: linha 237). Para `USER_ATTENDANT` (OWN): `user.getId().equals(null)` → false → **403 sempre**,
até na própria performance. Era passar `targetUserId` como owner em `getUserPerformance`.

### A5. Bug #11 — role `ADM_CLINICAL` não existe
`Role.java:4-5` tem só 8 roles; `ADM_CLINICAL` ausente e sem regras no seeder. Coordenador clínico
segue com 403 universal.

### A6. Regra de desconto / aprovação ausente (CLAUDE.md + contrato §7.6)
`DealServiceImpl.applyDiscount()` (166-207): nunca lê `conditions.maxDiscountPct` da `PermissionRule`
e **nunca lança `DiscountApprovalRequiredException`**. Seta `discountApprovedBy = user.getId()` em
**todo** desconto (linha 194), como se todos fossem auto-aprovados. A regra de ouro do CLAUDE.md
("desconto acima do limite → exceção") não está implementada. A exceção existe e está mapeada
(`GlobalExceptionHandler.java:49`), mas nunca é disparada.

---

## 🟡 MÉDIO — Regressões e divergências de contrato

### M1. `GET /tickets` ignora os filtros de query (regressão Fase 3)
`LeadTicketServiceImpl.search()` (235-255) só faz o `switch` por scope e **descarta** `customerId`,
`status` e `assignedTo`. O contrato §7.4 promete esses filtros com prioridade — o frontend que monta
`GET /tickets?status=...` recebe a lista inteira (paginada por scope), ignorando o parâmetro.
O próprio guia (linha 274) avisava: "os filtros precisam ser reaproveitados dentro do escopo base".

> **✅ Decisão tomada — ADR-013:** adotar JPA Specifications (scope + filtros compostos via `AND`,
> 1 SELECT + 1 COUNT). A premissa da ADR-012 ("filtros mutuamente exclusivos") caiu — scope é sempre
> aplicado e filtro é cumulativo. Filtros passam de "prioridade" para **cumulativos (AND)** — mudança
> de contrato já refletida em `frontend-integration-contract.md` §7.4/§15.

### M2. `Customer`/`ContactLog` search sem scope real
`CustomerServiceImpl.search()` (143-158) e `ContactLogServiceImpl.search()` (99-111) ainda usam o
auto-check (`checkOrThrow(..., user.getSector(), user.getId())`) — o anti-padrão que a ADR-012
apontou como trivialmente verdadeiro — e depois retornam `findAll()`. Um `USER_LEADS`/`USER_COMMERCIAL`
(scope OWN) enxerga **todos** os clientes/logs. Vazamento de dados entre OWNs.

> **✅ Decisão tomada — ADR-013:** `OWN → createdBy/userId`; `SECTOR`/`INTAKE → EXISTS` no `LeadTicket`
> (o setor é do ticket, não do cliente/log). Escolhido `EXISTS` em vez de denormalizar `currentSector`
> no Customer porque um cliente tem vários tickets em setores distintos ao longo do tempo (coluna
> ambígua + bug de sync). Sem migração de schema.

### M3. `closeDeal` curto-circuita a máquina de estados
`DealServiceImpl.closeDeal()` (233-241) seta `ticket.status = WIN` direto, sem validar que o ticket
está em `NEGOTIATION` (`ALLOWED_TRANSITIONS`), sem checar `TRANSITION_ROLES[WIN]` e sem gerar o
`ContactLog` automático que o `changeStatus` geraria. Tickets podem ir a WIN por caminho não previsto
e a auditoria (ContactLog) fica inconsistente com o contrato §10.

### M4. `CustomerServiceImpl.update()` não atualiza `phone2`
Linhas 134-138 setam name/cpf/email/phone, mas não `phone2` (presente no `CustomerUpdateRequest`,
contrato §7.3). Edição perde silenciosamente o telefone secundário.

---

## 🟢 BAIXO — Limpeza / código morto

### L1. `LeadTicketServiceImpl` — 4 métodos privados órfãos
Após a reescrita do `search()` para scope-aware, ficaram **sem uso**:
- `findAll(Pageable)` (258-262)
- `findByCustomer(UUID, Pageable)` (265-272)
- `findByStatus(TicketStatus, Pageable)` (275-278)
- `findByAssignedToUser(UUID, Pageable)` (281-288)

Consequência em cascata: `userRepository` só era usado por `findByAssignedToUser` → vira **dependência
órfã** (campo + parâmetro de construtor sem uso). Decidir junto de M1: se os filtros voltarem ao
`search()`, parte desses métodos é reaproveitada; se não, remover métodos + `userRepository`.

> Os privados `applyPostProcedure`, `applyScheduledReturn`, `applyLoss` (291-311) **estão em uso** no
> `changeStatus` — manter.

---

## ✅ O que está correto (não mexer)

- **INTAKE (ADR-011)** — `PermissionScope`, `resolveScope`, `getScope` e regras de seeder coerentes
  com a ADR. `getScope` com `Optional.empty()` para negação está bem modelado.
- **GlobalExceptionHandler** — 403/404/409/422/400/500 corretos, inclusive `AccessDeniedException → 403`
  (descarta a suspeita de 500 daqui no bug #15).
- **ContactLog (correção C1 do contrato)** — `statusBefore/After = null` em log manual
  (`ContactLogServiceImpl.java:71-72`).
- **Config (correções C2/C3 do contrato)** — GETs usam `CONFIGURE`; `getRecycle` usa
  `Optional.orElseThrow` → 404.
- **ADR-009 (timezone)** — cron do RecycleJob com `zone = "America/Sao_Paulo"`.
- **ADR-006 (anonimização)** — `phone="NULL"`, demais campos limpos, `anonymized=true`.
- **BigDecimal + HALF_UP** consistente em deal/analytics.
- `register()` em `UserServiceImpl` não está exposto em controller (sem auto-cadastro público) —
  porém é código morto que confunde; considerar remoção.

---

## Prioridade sugerida

| Ordem | Item | Por quê |
|-------|------|---------|
| 1 | C4 (DealHistory NOT NULL) | quebra todo o módulo de deals com 500 |
| 2 | C2 + C3 (RBAC e senha em Users) | falha de segurança ativa |
| 3 | C5 (RecycleJob NOT NULL) | feature de reciclo morta |
| 4 | A1–A5 (correções já especificadas no guia) | mecânicas, baixo risco |
| 5 | A6 (regra de desconto) | regra de negócio central faltando |
| 6 | M1 + L1 (search filtros + dead code) | regressão de contrato + limpeza |
| 7 | M2, M3, M4 | scope real, máquina de estados, phone2 |

**Operacional:** o `PermissionSeeder` tem early-return (`PermissionSeeder.java:49`); o `deleteAll()`
(linha 168) só roda com a tabela já vazia (inócuo). Em redeploy no Railway as regras antigas persistem
— executar `DELETE FROM permission_rules` (Bloco 1E do guia) antes de validar qualquer correção de RBAC,
senão testa-se a matriz velha.

---

## Referências cruzadas

- `impl-backend-corrections-sprint1.md` — guia de correções (apenas Bloco 1A aplicado)
- `bugs-producao-railway-2026-06-05.md` — bugs #2/#5/#6/#11/#14/#15/#16/#17
- `ADR-011` — escopo INTAKE (implementado)
- `ADR-012` — padrão list/scope-aware (parcial; ver M1/M2)
- `frontend-integration-contract.md` — §7.3 (phone2), §7.4 (filtros tickets), §7.6 (desconto), §8 (RBAC)