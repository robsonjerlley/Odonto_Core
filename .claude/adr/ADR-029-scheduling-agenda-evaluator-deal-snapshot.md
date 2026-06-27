# ADR-029: Módulo `scheduling` — Agenda do Evaluator a partir do Deal fechado

**Status**: Proposto (RASCUNHO — aguardando 3 definições do dono do produto antes de fechar)
**Data**: 2026-06-27
**Autores**: Arquiteto-Agent + Robson
**Impacto**: novo módulo `scheduling`; `commercial` (novo read-boundary `DealProvider`); reuso de `catalog` (`ProcedureProvider`), `identity` (role `Evaluator`)
**Relaciona**: ADR-023 (TicketWonEvent — fica como hook futuro, NÃO no MVP), ADR-026/028 (catálogo + read-model pattern), ADR-024 (`@TenantId`), ADR-003 (imutabilidade/trilha), ADR-002 (interface vs impl)

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
`scheduling` **não** injeta `DealRepository` nem `ProcedureRepository`.

```
scheduling ──▶ commercial   (novo DealProvider / WonDealView — read-model dos procedimentos do deal fechado)
scheduling ──▶ catalog      (ProcedureProvider — estimatedDuration; já existe)
scheduling ──▶ identity     (role Evaluator)
```

Dependência unidirecional. `commercial` precisa expor um provider de leitura (espelho do que `catalog` fez na ADR-028) para que a regra "o que é um deal agendável" continue morando no `commercial`.

### Fonte: PULL no MVP, não evento (🎯)
O fluxo descrito é **pull**: o usuário abre a agenda, escolhe os procedimentos do deal won e marca. Não exige o `TicketWonEvent`.
- **MVP**: `scheduling` lê won-deals via `DealProvider` sob demanda.
- **Futuro**: `TicketWonEvent` (ADR-023) vira hook para comportamento proativo (worklist automática ao fechar deal, notificações). Quando chegar, Spring `ApplicationEvent` in-process — sem outbox/broker nesta escala.
- ADR-023 permanece válida como evolução; não bloqueia o MVP.

### Trilha de auditoria (consistência ADR-003 / DealHistory)
Remarcar/cancelar **não mutam destrutivamente**: `status` + timestamps + `cancelReason`. Trilha leve, sem event sourcing.

---

## Tensões de modelagem — recomendações pendentes de decisão (⚠️)

> Estas 3 são as perguntas a responder na PRÓXIMA SESSÃO antes de cravar o modelo de dados.

### Q1 — Escopo da agenda
Avaliação (`IN_EVALUATION`) e procedimento (Deal won) têm **fontes diferentes** mas mesma agenda visual.
- 📐 Recomendação: `Appointment` com `type` (`EVALUATION | PROCEDURE`) desde o dia 1 (enum barato, evita migração). Alternativa enxuta: MVP só `PROCEDURE`.
- **DECISÃO: [ pendente ]**

### Q2 — Evaluator é sempre o executor?
O avaliador que fechou o Deal é sempre quem executa as sessões? Em clínica nem sempre.
- 📐 Recomendação: se "sempre", vincular à `Evaluator`; se "talvez não", já separar `assignedTo` (quem atende) do avaliador original — um campo agora vs. refactor depois.
- **DECISÃO: [ pendente ]**

### Q3 — Cardinalidade sessões × horário
Um atendimento tem 1 horário. Procedimento de N sessões = N horários. "Quantidade de sessões" num registro de 1 data/hora não fecha (quando ocorrem as sessões 2 e 3?).
- 🎯 Recomendação: **`Appointment` = 1 slot atômico (1 data/hora)**. "Sessões" vira número de planejamento; cada sessão é um `Appointment` próprio, remarcável/concluível individualmente.
- **DECISÃO: [ pendente ]**

---

## Esboço do modelo (sujeito às 3 decisões acima)

```
Tabela: crm_db.appointments
─────────────────────────────────────────────────────────
id              : UUID          PK
clinic_id       : UUID          NOT NULL — @TenantId (ADR-024)
type            : VARCHAR       EVALUATION | PROCEDURE        (depende de Q1)
ticket_id       : UUID          nullable — origem (avaliação ou deal)
deal_id         : UUID          nullable — quando type=PROCEDURE
procedure_id    : UUID          nullable — FK lógica ao catálogo (snapshot via Deal)
customer_id     : UUID          NOT NULL
evaluator_id    : UUID          NOT NULL — quem fechou/avaliou
assigned_to     : UUID          nullable — quem atende, se ≠ evaluator (depende de Q2)
scheduled_at    : TIMESTAMP     NOT NULL — data/hora do slot
estimated_min   : INTEGER       nullable — vem de Procedure.estimatedDuration
status          : VARCHAR       SCHEDULED | DONE | CANCELLED
planned_sessions: INTEGER       nullable — nº de planejamento (depende de Q3)
note            : TEXT          nullable
cancel_reason   : TEXT          nullable
created_by      : UUID          NOT NULL
created_at      : TIMESTAMP     NOT NULL
updated_at      : TIMESTAMP     NOT NULL
```

Máquina de estados: `SCHEDULED → DONE` (concluir) | `SCHEDULED → CANCELLED` (cancelar, exige `cancel_reason`) | remarcar = novo `Appointment` referenciando o anterior (ou update de `scheduled_at` com timestamp — definir junto da trilha).

Operações REST (a detalhar): criar, listar (agenda por evaluator/período, scope-aware ADR-013/015), remarcar, cancelar, concluir.

---

## Estrutura de módulo prevista

```
modules/
  scheduling/                 ← novo módulo
    domain/model/Appointment.java          (@TenantId)
    repository/AppointmentRepository.java  + Specifications (ADR-013)
    service/AppointmentService.java        (interface — ADR-002)
    service/impl/AppointmentServiceImpl.java
    api/controller/AppointmentController.java
    api/dto/...
  commercial/
    provider/DealProvider.java  + WonDealView   ← NOVO read-boundary (espelha ADR-028)
  catalog/                    ← reuso ProcedureProvider (existente)
```

---

## Atualização 2026-06-27 — sessão de design (ver ADR-030)

Sessão multi-agente (Arquiteto + P.O. + UI/UX) avançou várias definições. Decididos:
- **Q1**: agenda tem `EVALUATION` (avaliação fica no ticket via `scheduledAt`) **e** `PROCEDURE` (só nasce no WIN). `type` desde o dia 1.
- **Q2**: `assigned_to` (executor) **mutável** e separado de `evaluator_id` (imutável, auditoria); default `assigned_to = evaluator`. Flexibilidade é requisito de mercado (clínica pequena), não over-engineering.
- **Q3**: `Appointment` = 1 slot atômico; `DealProcedure.quantity` = nº de sessões → N `Appointment` (1 por sessão).
- **Gatilho**: Fluxo A — evento `DealWonEvent` **in-process síncrono na mesma transação** do `closeDeal` (fail-fast). Promove a **ADR-023 ao MVP**.
- **Estado novo**: `AWAITING_SCHEDULE` (nasce sem data no WIN) → `SCHEDULED → DONE | CANCELLED`.
- **customer_id**: vem sempre do ticket (não snapshot no Deal).

> 📌 **PONTEIRO — contratos REST pendentes**: as necessidades de API derivadas do frontend estão na **ADR-030 (UX), seção 6 "[IMPACTO BACKEND]"** (worklist `AWAITING_SCHEDULE`, agenda do dia por executor, `schedule`/`assignee`/remarcar/cancelar/concluir, batch de sessões, conflito de horário, status de pagamento, regra do perfil "solo"). Cópia espelhada em `frontend/docs/adr-frontend-002-...`. Fechar esses contratos é o próximo trabalho de arquitetura.

## Próximo passo (retomar aqui)
1. Responder Q1, Q2, Q3.
2. Fechar o modelo de dados + máquina de estados (incl. decisão remarcar = novo registro vs. update).
3. Definir o contrato `DealProvider`/`WonDealView` no `commercial`.
4. Contrato REST do `AppointmentController` (API-first, ADR-001) + acessos (RBAC, ADR-012).
5. Migration Flyway `V[n]__create_appointments.sql`.
6. Promover esta ADR de RASCUNHO → Aceito.
