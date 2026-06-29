# ADR-029: Módulo `appointment` — Agenda do Evaluator a partir do Deal fechado

**Status**: **Aceito** (2026-06-27) — decisões + formalização FECHADAS (Q1/Q2/Q3 + Travamentos A–D + contrato `DealWonEvent` + REST `AppointmentController`). Migration **não se aplica** (projeto roda local, `ddl-auto=update`; tabela nasce do entity — Flyway só no 1º deploy). Pronta para implementação.
**Data**: 2026-06-27
**Autores**: Arquiteto-Agent + Robson
**Impacto**: novo módulo `appointment`; `commercial` (publica `DealWonEvent` no `closeDeal`); reuso de `catalog` (`ProcedureProvider`), `identity` (role `Evaluator`)
**Relaciona**: ADR-023 (TicketWonEvent — **substituída por esta ADR**; o gatilho vigente é o `DealWonEvent` síncrono), ADR-026/028 (catálogo + read-model pattern), ADR-024 (`@TenantId`), ADR-031 (`Deal.paymentStatus` — consumido no feed, não definido aqui), ADR-003 (imutabilidade/trilha), ADR-002 (interface vs impl)

---

> # 🎯 Direcionamento de Implementação — leia isto para codar
>
> Módulo **`appointment`** (`modules/appointment`) — agenda do Evaluator. Decisões abaixo estão **fechadas**; o histórico de como se chegou nelas está no rodapé (📜). Esta ADR **substitui a ADR-023** quanto ao gatilho de fechamento.
>
> **Modelo (essencial):**
> - Tabela `appointments` é **PROCEDURE-only**. `EVALUATION` é projeção de leitura do ticket (`funnel`) — **não vira linha**.
> - 1 `Appointment` = 1 slot (1 data/hora). `DealProcedure.quantity = N` → **N appointments** (`session_index 1..N`, `planned_sessions = N`).
> - Estados: `AWAITING_SCHEDULE → SCHEDULED → DONE | CANCELLED`. **Remarcar = update de `scheduled_at`** (não cria registro novo).
> - `evaluator_id` imutável (= `deal.createdBy`); `assigned_to` mutável (default = `evaluator_id`).
> - Snapshot no WIN: `customer_name`, `procedure_name`, `estimated_min` (nullable).
>
> **Gatilho (NÃO é a ADR-023):** `DealWonEvent` publicado em `DealServiceImpl.closeDeal`, **síncrono / mesma transação**, consumido por `AppointmentEventListener` (`@EventListener` — NÃO `@Async`, NÃO `AFTER_COMMIT`). Throw → **rollback do `closeDeal`** (fail-fast). Tenant já está no contexto → **sem** `TenantContext.set`.
>
> **Checklist:**
> 1. `core/events/DealWonEvent.java` — record (conferir contrato na seção "Modelo de dados").
> 2. `appointment/domain/model/Appointment.java` — `@Entity` + `@TenantId`, `@Table(name="appointments", schema="crm_db", indexes={status; (assignedTo, scheduledAt)})`. Tabela nasce do entity (`ddl-auto=update`) — **sem migration** (ADR-027).
> 3. `appointment/repository/AppointmentRepository.java` + Specifications scope-aware (ADR-013).
> 4. `appointment/event/AppointmentEventListener.java` — síncrono; cria N appointments `AWAITING_SCHEDULE`.
> 5. `commercial/DealServiceImpl.closeDeal` — publica `DealWonEvent` (resolve `customerName` via `CustomerProvider`; `estimatedDuration` via `ProcedureProvider.resolveActiveByIds`).
> 6. `funnel/provider/CustomerProvider` + `CustomerView` — read-boundary do nome do paciente (padrão ADR-028).
> 7. `AppointmentService`/`Impl` + `AppointmentController` (REST na seção "Contratos REST") + DTOs.
> 8. RBAC: `Resource.APPOINTMENT` (novo valor no enum) + seed `PermissionRule` (tabela na seção RBAC).
>
> **Fronteira:** `appointment` só CONSOME — não injeta `DealRepository`/`ProcedureRepository`, **nunca importa `funnel`**. ⚠️ `Deal` **não tem `@TenantId`** — verificar isolamento antes de montar o evento.

---

## Contexto

Próximo item do roadmap (item 1), destravado pelo fechamento do catálogo (ADR-026/028 — `Procedure.estimatedDuration` agora disponível). O `Evaluator` (exibido no frontend como "Especialista clínico") precisa de uma agenda para:
- organizar as **avaliações** (tickets em `IN_EVALUATION`), e
- agendar os **procedimentos** lançados em um `Deal` com status fechado (won).

Proposta do dono do produto: contexto enxuto — pegar os procedimentos do Deal fechado, adicionar à agenda com data/hora + campos opcionais (quantidade de sessões, anotações); operações de alterar/remarcar/cancelar/concluir atendimento. Materiais de insumo = feature futura, ainda não decidida.

---

## Decisões já validadas (✅)

1. **Reusar a role `Evaluator`** (não criar role/entidade "Especialista clínico" nova) — evita proliferação de papéis; frontend já integra com esse rótulo.
2. **`Deal` fechado (won) como fonte** dos procedimentos agendáveis.
3. **Ciclo de vida como máquina de estados** (`SCHEDULED → DONE | CANCELLED`, com `RESCHEDULED` via novo registro).
4. **Materiais de insumo NÃO entram agora** — `Appointment` fica extensível, sem acoplar nada que impeça adicionar `materials` depois. Modelar isso já seria over-engineering.

---

## Decisões de fronteira / arquitetura (📐)

### Fronteira de módulo (read-boundary, padrão ADR-028)
`appointment` **não** injeta `DealRepository` nem `ProcedureRepository`.

```
appointment ◀──── commercial   (DealWonEvent — push, event-carried state transfer; ver "Modelo de dados")
appointment ──▶ catalog        (ProcedureProvider — estimatedDuration; já existe)
appointment ──▶ identity       (role Evaluator)
```

Dependência unidirecional. A fonte dos procedimentos agendáveis é o **`DealWonEvent`** (push síncrono), não um read-pull — o `appointment` recebe o estado completo no evento e não chama de volta o `commercial`. **Não há `DealProvider`/`WonDealView` de procedimentos** (ver decisão abaixo).

> 🔻 O único read do `commercial` que sobrevive é o `Deal.paymentStatus` consumido pela Home "Pagamentos pendentes" (Travamento B) — **escopo da ADR-031**, não desta ADR, e não toca o agregado `Appointment`.

### ~~Fonte: PULL no MVP, não evento~~ — ❌ SUPERADA (2026-06-27)
> **Esta seção foi revertida.** A decisão fechada na mesma sessão (ver "Modelo de dados" e "Modelo consolidado") é **PUSH via `DealWonEvent`** síncrono, in-process, na **mesma transação** do `closeDeal` (fail-fast). Promove a ADR-023 ao MVP. O texto abaixo fica só como registro histórico do que foi considerado e descartado.

~~O fluxo descrito é **pull**: o usuário abre a agenda, escolhe os procedimentos do deal won e marca. Não exige o `TicketWonEvent`.~~
- ~~**MVP**: `appointment` lê won-deals via `DealProvider` sob demanda.~~
- ~~**Futuro**: `TicketWonEvent` (ADR-023) vira hook para comportamento proativo.~~

**Por que o push venceu:** o agendamento precisa que as N sessões já existam como `AWAITING_SCHEDULE` no instante do WIN (worklist "A agendar"), e o evento carrega o estado completo — elimina o read-pull e a necessidade de um `DealProvider` de procedimentos. ADR-023 deixa de ser "futuro" e entra no MVP.

### Trilha de auditoria (consistência ADR-003 / DealHistory)
Remarcar/cancelar **não mutam destrutivamente**: `status` + timestamps + `cancelReason`. Trilha leve, sem event sourcing.

---

## Tensões de modelagem — DECIDIDAS (✅ 2026-06-27, sessão multi-agente / ADR-030)

### Q1 — Escopo da agenda
Avaliação (`IN_EVALUATION`) e procedimento (Deal won) têm **fontes diferentes** mas mesma agenda visual.
- **DECISÃO**: `Appointment` com `type` (`EVALUATION | PROCEDURE`) **desde o dia 1**. `EVALUATION` reflete a avaliação no ticket (via `scheduledAt`); `PROCEDURE` só nasce no WIN do Deal.

### Q2 — Evaluator é sempre o executor?
O avaliador que fechou o Deal é sempre quem executa as sessões? Em clínica nem sempre.
- **DECISÃO**: separar `evaluator_id` (imutável, auditoria de quem fechou/avaliou) de `assigned_to` (executor, **mutável**), com `assigned_to = evaluator` por default. Flexibilidade é requisito de mercado de clínica pequena, não over-engineering.

### Q3 — Cardinalidade sessões × horário
Um atendimento tem 1 horário. Procedimento de N sessões = N horários.
- **DECISÃO**: **`Appointment` = 1 slot atômico (1 data/hora)**. `DealProcedure.quantity` = nº de sessões → gera **N `Appointment`** (1 por sessão), cada um remarcável/concluível individualmente. `planned_sessions` deixa de ser campo de cardinalidade e vira apenas rótulo de exibição "Sessão X de N".

---

## Modelo de dados (FECHADO — alinhado às decisões Q1/Q2/Q3 e à ADR-030)

```
Tabela: crm_db.appointments
─────────────────────────────────────────────────────────
id              : UUID          PK
clinic_id       : UUID          NOT NULL — @TenantId (ADR-024)
type            : VARCHAR       EVALUATION | PROCEDURE                (Q1)
ticket_id       : UUID          NOT NULL — origem (avaliação ou deal→ticket)
deal_id         : UUID          nullable — quando type=PROCEDURE
procedure_id    : UUID          nullable — FK lógica ao catálogo (snapshot via Deal)
procedure_name  : VARCHAR       nullable — snapshot do nome no WIN (Travamento A)
customer_id     : UUID          NOT NULL — SEMPRE resolvido do ticket (não snapshot do Deal)
customer_name   : VARCHAR       NOT NULL — snapshot do nome no WIN; deep-link via customer_id p/ canônico (Travamento A)
evaluator_id    : UUID          NOT NULL — imutável; quem fechou/avaliou (auditoria)   (Q2)
assigned_to     : UUID          NOT NULL — executor, MUTÁVEL; default = evaluator_id    (Q2)
scheduled_at    : TIMESTAMP     nullable — NULL enquanto AWAITING_SCHEDULE; setado no schedule
estimated_min   : INTEGER       nullable — snapshot de Procedure.estimatedDuration (pode ser NULL)
status          : VARCHAR       AWAITING_SCHEDULE | SCHEDULED | DONE | CANCELLED
session_index   : INTEGER       nullable — "X" em "Sessão X de N" (Q3)
planned_sessions: INTEGER       nullable — "N" de exibição; NÃO é cardinalidade
note            : TEXT          nullable
cancel_reason   : TEXT          nullable — obrigatório quando status=CANCELLED
created_by      : UUID          NOT NULL
created_at      : TIMESTAMP     NOT NULL
updated_at      : TIMESTAMP     NOT NULL
```

**Origem dos registros**:
- `PROCEDURE` nasce no **WIN do Deal**, via `DealWonEvent` (in-process, síncrono, **mesma transação** do `closeDeal` — fail-fast). Promove a ADR-023 ao MVP. `DealProcedure.quantity = N` → N appointments em `AWAITING_SCHEDULE`.
- `EVALUATION` **não é linha persistida** — é **projeção de leitura** do ticket (`funnel`): para o Evaluator, o `ticketStatus` agendado **já é** o agendamento. A agenda consome o ticket; a tabela `appointments` é **PROCEDURE-only**.

**Contrato `DealWonEvent` (event-carried state transfer — evento sai completo, `appointment` não faz callback):**
```java
// core/events/DealWonEvent.java
public record DealWonEvent(
    UUID clinicId,          // explícito p/ log e transporte-agnóstico
    UUID dealId, UUID ticketId,
    UUID customerId, String customerName,   // customerName snapshotado no WIN (Travamento A)
    UUID evaluatorId,       // = deal.createdBy → default evaluator_id E assigned_to
    LocalDateTime closedAt,
    List<WonProcedure> procedures
) {
    public record WonProcedure(UUID procedureId, String name, String code,
                               int quantity, Integer estimatedDuration) {}
}
```
- `evaluatorId = deal.createdBy` — o `Deal` só é criado por **Evaluator** ou **ADM_SYSTEM**, então `createdBy` é sempre o dono natural da agenda (default de `evaluator_id` e `assigned_to`). `closedBy` (commercial — quem fechou o contrato) **não entra**: não tem relação com a execução.
- `customerName` → lido via **novo `CustomerProvider`** read-only no `funnel` (padrão ADR-028; o nome só existe em `Customer`).
- `estimatedDuration` por procedimento → resolvido via `ProcedureProvider.resolveActiveByIds(...)` (já injetado no `DealServiceImpl`); procedimento desativado desde a venda → `null` (aceito — `estimated_min` é nullable).

**Publicação** — `DealServiceImpl.closeDeal`, dentro do `@Transactional`, após o save do ticket WIN + deal, antes do `return`:
```java
applicationEventPublisher.publishEvent(/* DealWonEvent montado com customerName + durations */);
```
**Consumo** — `AppointmentEventListener` no `appointment`, `@EventListener` **síncrono** (NÃO `@Async`, NÃO `AFTER_COMMIT`): cria `quantity` appointments `AWAITING_SCHEDULE` por procedimento (`session_index = 1..N`, `planned_sessions = N`), na **mesma transação**; throw → **rollback do `closeDeal`** (fail-fast). O `appointment` **nunca importa `funnel`** — só consome o evento.

> 📐 Por ser síncrono na thread/transação do request, o `TenantContext` já está setado — o `@TenantId` preenche o `clinicId` dos appointments sozinho, **sem** o `TenantContext.set/clear` que a ADR-023 exige nos seus listeners `@Async` pós-commit.

**Máquina de estados**:
```
AWAITING_SCHEDULE ──schedule(data/hora)──▶ SCHEDULED ──concluir──▶ DONE
                                              │
                                              └──cancelar(cancel_reason)──▶ CANCELLED
```
- **Remarcar = update de `scheduled_at`** no mesmo registro + `updated_at` (trilha leve ADR-003). **NÃO** gera novo `Appointment` (decisão fechada: novo registro só existe por sessão, não por remarcação — evita inflar a tabela e quebra a contagem "X de N").
- `EVALUATION` pode nascer já `SCHEDULED` (avaliação já tem data no ticket).

---

## Estrutura de módulo prevista

```
modules/
  core/events/
    DealWonEvent.java           ← NOVO: contrato do evento (record, transporte-agnóstico)
  appointment/                 ← novo módulo
    domain/model/Appointment.java          (@TenantId)
    repository/AppointmentRepository.java  + Specifications (ADR-013)
    service/AppointmentService.java        (interface — ADR-002)
    service/impl/AppointmentServiceImpl.java
    event/AppointmentEventListener.java    ← NOVO: @EventListener síncrono do DealWonEvent (fail-fast)
    api/controller/AppointmentController.java
    api/dto/...
  commercial/
    service/impl/DealServiceImpl.java      ← publica DealWonEvent no closeDeal (sem DealProvider — push, não pull)
  funnel/
    provider/CustomerProvider.java + CustomerView  ← NOVO read-boundary (nome do paciente; padrão ADR-028)
  catalog/                    ← reuso ProcedureProvider (existente)
```

---

## Contratos REST — FECHADO (`AppointmentController`, base `/api/v1/appointments`)

Derivado da **ADR-030 (UX), seção 6 "[IMPACTO BACKEND]"**. Aterrissado no RBAC real: `PermissionService.checkOrThrow(user, resource, action, targetSector, targetOwnerId)` + `PermissionScope {GLOBAL, SECTOR, OWN, INTAKE}`, data-driven via `PermissionRule` (ADR-012).

### DTO de resposta (worklist + agenda — mesma forma; backend devolve N linhas chapadas, **o front agrupa** por `dealId`+`procedureId` — Travamento C)
```java
AppointmentResponseDTO(
  UUID id, AppointmentType type,            // PROCEDURE (EVALUATION é projeção do funnel, não trafega aqui)
  UUID dealId, UUID procedureId, String procedureName,
  UUID customerId, String customerName,
  UUID evaluatorId, UUID assignedTo,
  AppointmentStatus status,                 // AWAITING_SCHEDULE | SCHEDULED | DONE | CANCELLED
  LocalDateTime scheduledAt,                // null em AWAITING_SCHEDULE
  Integer estimatedMin,
  Integer sessionIndex, Integer plannedSessions,   // "Sessão X de N" — p/ o front agrupar
  String note, String cancelReason
)
```

### Endpoints
`targetOwnerId = appointment.assignedTo`, `targetSector = EVALUATOR` (resolvidos no service antes do `checkOrThrow`).

| Verbo + path | Corpo | Transição / regra | Status | Action |
|---|---|---|---|---|
| `GET /appointments?status=AWAITING_SCHEDULE` | — | worklist "A agendar"; N linhas chapadas | 200 | `READ` |
| `GET /appointments?assignedTo=&from=&to=` | — | agenda por executor/dia — **só PROCEDURE** (EVALUATION vem do funnel; front funde) | 200 | `READ` |
| `PATCH /appointments/{id}/schedule` | `{scheduledAt, assignedTo?}` | `AWAITING_SCHEDULE → SCHEDULED` | 200 · **422** se data no passado ou status≠AWAITING | `UPDATE` |
| `PATCH /appointments/schedule-batch` | `{items:[{appointmentId, scheduledAt, assignedTo?}]}` | atômico tudo-ou-nada; conflito de horário = `warnings[]` (não bloqueia) | 200 · **422** (nada aplicado) | `UPDATE` |
| `PATCH /appointments/{id}/reschedule` | `{scheduledAt}` | `SCHEDULED → SCHEDULED` (update + trilha leve ADR-003) | 200 · **422** se status≠SCHEDULED | `UPDATE` |
| `PATCH /appointments/{id}/assignee` | `{assignedTo}` | reatribui executor (`assigned_to` mutável) | 200 | `UPDATE` |
| `PATCH /appointments/{id}/cancel` | `{cancelReason}` | `→ CANCELLED`; reason **obrigatório** | 200 · **422** sem reason | `UPDATE` |
| `PATCH /appointments/{id}/complete` | — | `SCHEDULED → DONE` | 200 · **422** se status≠SCHEDULED | `UPDATE` |

> **Tudo é `UPDATE` de propósito** (não há `CLOSE` separado p/ `complete`): a operação exige flexibilidade — recepção (`USER_ATTENDANT`) também marca/remarca/conclui. A diferença entre papéis fica **no scope**, não na action.

### RBAC — seed `PermissionRule` (Resource=`APPOINTMENT`, **novo valor no enum**)
| Role | Actions | Scope |
|---|---|---|
| `USER_EVALUATOR` | READ, UPDATE | `OWN` (`assignedTo == user.id`) — própria agenda |
| `ADM_EVALUATOR` | READ, UPDATE | `SECTOR` (EVALUATOR) |
| `USER_ATTENDANT` | READ, UPDATE | `GLOBAL` (recepção; tenant-bound pelo `@TenantId`) |
| `ADM_SYSTEM` | READ, UPDATE | `GLOBAL` |

> **Fora do escopo do `appointment`:** o feed de pagamentos (ADR-030 #9) é da Home/financeiro (→ ADR-031) — não expõe endpoint aqui. O atributo de perfil "solo/equipe" (ADR-030 #10) é do onboarding da clínica (Travamento D), não do `appointment`.

## Modelo consolidado (fechamento da sessão 2026-06-27)

Princípio: **`appointment` é uma agenda que só CONSOME dos outros módulos.** Não cria conceito novo, não escreve fora do seu agregado.

- **Dono da agenda = o profissional (Evaluator/executor), não o Deal nem o Customer.** Ciclo do appointment é **independente do Deal**: falta/desmarque não toca o Deal. Mudança de Deal (compra parcelada, abandono, cancelamento) é `commercial`/financeiro — fora daqui.
- **Duas fontes, uma view unificada:**
  - `EVALUATION` → projeção do ticket (`funnel`); `ticketStatus` agendado **é** o agendamento. Não vira linha.
  - `PROCEDURE` → linha persistida em `appointments`, nascida do `DealWonEvent` (commercial). Cada sessão = um appointment próprio.
- **Sem estado/campo novo desnecessário:** não há `NO_SHOW` nem campo extra no ticket — falta/reentrada usa o mecanismo **`RECYCLE`/`RECYCLED`** que já existe. Cada novo agendamento é um agendamento novo.
- **RBAC** (roles reais): `USER_EVALUATOR` = própria agenda; `ADM_EVALUATOR` = setor EVALUATOR; `USER_ATTENDANT` = recepção marca/remarca; `ADM_SYSTEM` = global.
- **Travamentos A–D**: todos resolvidos (A snapshot via `DealWonEvent`; B → ADR-031; C batch; D perfil no onboarding).

---

## 📜 Histórico de Decisão

> Tudo abaixo é **registro** de como as decisões foram fechadas (checklist de fechamento + Travamentos A–D, todos resolvidos) e o caminho pull-vs-push considerado e descartado. Para **implementar**, use o bloco 🎯 do topo + as seções "Modelo de dados" e "Contratos REST". **Não há ação pendente aqui.**

### Fechamento (checklist original — tudo ✅)
1. ~~Responder Q1, Q2, Q3~~ ✅ — fechadas acima.
2. ~~Modelo de dados + máquina de estados~~ ✅ — fechados acima (remarcar = update).
3. ~~Resolver os Travamentos A–D~~ ✅ **todos resolvidos** (ver seção Travamentos).
4. ~~Definir o contrato do **`DealWonEvent`**~~ ✅ — fechado (ver "Contrato `DealWonEvent`"). Publisher no `commercial.closeDeal` + `@EventListener` síncrono no `appointment`.
5. ~~Contrato REST do `AppointmentController`~~ ✅ — fechado (ver "Contratos REST — FECHADO"). Tudo `UPDATE`, scope por papel; `Resource.APPOINTMENT` novo no enum.
6. ~~Migration Flyway `V[n]__create_appointments.sql`~~ ❌ **não se aplica agora.** O projeto **não está em deploy — roda local**, com `ddl-auto=update` (Flyway desabilitado em local; `application-local.properties:7`). A tabela `appointments` **nasce do `@Entity Appointment`** (`@Table(name="appointments", schema="crm_db")` + `@TenantId clinicId`), igual a `deals`/`lead_tickets` — não há migration de tabela no projeto. **Índices dos hot paths** (`status`; `assigned_to, scheduled_at`) declarados via `@Table(indexes=…)` no entity, criados pelo `ddl-auto`. **Flyway fica para o primeiro deploy:** quando o schema estabilizar, migra-se tudo de uma vez para Flyway-owned DDL + `ddl-auto=validate` (dívida já registrada em `application-prod.properties:15`). ← **próximo: implementar o entity + módulo, não SQL**
7. ~~Promover esta ADR de RASCUNHO → Aceito.~~ ✅ **Aceito em 2026-06-27.** Próximo trabalho = implementação (entity + módulo `appointment`, `DealWonEvent`, `CustomerProvider`, RBAC seed).

## Travamentos (⚠️ decisões que bloqueiam fechar os contratos REST)

> Status: **A, B, C, D ✅ todos resolvidos.** Travamentos não bloqueiam mais os contratos REST.

### Travamento A — Nomes resolvidos ✅ RESOLVIDO (2026-06-27)
**Pergunta:** worklist exibe nome de paciente/procedimento, mas `Appointment` guarda só IDs. Snapshot no WIN vs. resolver no read?

**Constatações no código:**
- `DealProcedure` (commercial) **já snapshota** `name`/`code`/`tableValue` na venda → nome do procedimento já é dado congelado.
- `Deal` carrega só `ticketId`; `LeadTicket` (funnel) carrega só `customerId` — **ninguém materializa o nome do paciente**; ele só existe em `Customer` (funnel).
- Não existe `CustomerProvider`; o nome exige cruzar pro `funnel` de qualquer forma — a única escolha é **quando** (1x no WIN vs. a cada read).

**Decisão (a) — snapshot no WIN, via `DealWonEvent` completo:** denormaliza `customer_name` e `procedure_name` no `Appointment`, congelados no `closeDeal`. Mantém `customer_id`/`procedure_id` como ponteiro p/ deep-link ao cadastro canônico.
- ✅ Worklist/agenda (hot path da Home) = query single-table, sem N joins nem chamadas a provider por linha.
- ✅ `appointment` autônomo no read; acoplamento cross-módulo só no write (1x, no WIN). `appointment` nunca importa `funnel`.
- ✅ Consistente com ADR-003 e com o snapshot que `commercial` já faz.
- ⚠️ Rename de paciente não propaga a appointments já criados. Aceito: raro; `customer_id` permite deep-link ao canônico. (b) "resolver no read" só se justificaria se "nome sempre canônico na agenda" fosse requisito duro — não é.

**Como o `commercial` obtém o `customerName` (sub-decisão de fronteira):**
- O nome é **read novo** → fazer **limpo**: novo `CustomerProvider` read-only no `funnel` (padrão ADR-028). Barato (1 interface + impl) e **não** adiciona dívida. Injetar `CustomerRepository` direto foi descartado: aumentaria a superfície suja.
- 🔻 **DÍVIDA REGISTRADA (não refatorar agora):** o `commercial` já acopla o `funnel` por **write-coupling** — `DealServiceImpl.create`/`closeDeal` mutam o `LeadTicket` e o `RecycleJob` faz `new LeadTicket()` (cria agregado do funnel). Isso fura a ADR-028, mas é **write**, não read, e **não tem relação com appointment**. Refatorar (inverter p/ eventos/comandos donos no funnel) é pesado e off-scope desta feature. Fica como débito técnico separado.

### Travamento B — Status de pagamento no feed ✅ RESOLVIDO (2026-06-27)
**Fora do escopo:** agendamento não tem nada a ver com financeiro/commercial. A tabela `appointments` não ganha campo de pagamento; o feed "Pagamentos pendentes" (ADR-030 §4) é da Home e a decisão do `paymentStatus` mora no `commercial` → **ver ADR-031.**

### Travamento C — "Planejar N sessões": batch ✅ RESOLVIDO (2026-06-27)
**Reenquadramento:** pela Q3, as N sessões **já existem** como N `Appointment` em `AWAITING_SCHEDULE` (nascem no WIN). Logo "planejar N" é **update em lote de `scheduled_at`**, não insert.

**Agrupamento é responsabilidade do FRONT (não do backend):** a worklist mostra 1 linha por procedimento (agrupando as sessões `AWAITING_SCHEDULE` do mesmo `dealId`+`procedureId`), mas isso é decisão visual. O backend devolve as **N linhas chapadas** e o front agrupa — backend não tem lógica de agrupamento.
- **Consequência de contrato (o que de fato entra aqui):** a resposta da worklist (#1/#2) **deve expor** `dealId`, `procedureId`, `session_index`, `planned_sessions` para o front poder agrupar e segurar os N `appointmentId`.

**Contrato do batch:**
```
PATCH /appointments/schedule-batch              (scope-aware, ADR-013/015)
body: { items: [ { appointmentId, scheduledAt, assignedTo? } ] }
```
- **Atômico tudo-ou-nada:** qualquer `scheduledAt` no passado, ou `appointmentId` que não esteja em `AWAITING_SCHEDULE` → **422, nada aplicado**. (Resolve reentrada: o front só envia as sessões ainda `AWAITING_SCHEDULE`; as já `SCHEDULED` são rejeitadas → consistência.)
- **Conflito de horário = warning, não bloqueia** (ADR-030): aplica e retorna `warnings[]`.
- **`assignedTo` opcional por item**, default = `assigned_to` atual (que cai em `evaluator`).
- **Datas explícitas vindas do front** — recorrência ("a cada k dias") é calculada/editada na tela; o **backend não calcula recorrência**.
- **1 sessão (default da ADR-030)** usa `PATCH /appointments/{id}/schedule` (#5); o batch é só para o opt-in "N de uma vez".

### Travamento D — Detecção do perfil "solo/papel amplo" ✅ RESOLVIDO (2026-06-27)
**Pergunta:** a Home Modo Operação depende de saber se o usuário é "solo". Critério e origem do dado não estavam definidos.

**Constatação:** o `User` tem **1 `Sector`** (`user.getSector()`), não acumula papéis — então "amplitude de papel" não é representável no modelo atual; e contagem de usuários da clínica é proxy frágil (home muda sozinha ao contratar o 2º).

**Decisão (opção 3 — atributo declarado + default agressivo):**
- **Critério:** perfil **declarado no onboarding** da clínica (1 pergunta no cadastro: "trabalha sozinho ou em equipe?") → novo atributo de nível de clínica.
- **Default enquanto o campo não existir:** **todo mundo vê a Home Modo Operação.** O público-alvo estratégico é a clínica pequena, então o erro é pro lado certo.
- ⚠️ **Guardião de escopo:** auto-detecção (heurística por papel/contagem) é **scope creep** — o valor está no feed em si, não no mecanismo que escolhe entre duas homes. Uma heurística que erra entrega a home errada (pior que não ter). Auto-detecção vira **Could-have**, depois que houver dado de uso.
- **Impacto no frontend:** destrava implementar **uma** home no MVP (não duas + regra de seleção).
