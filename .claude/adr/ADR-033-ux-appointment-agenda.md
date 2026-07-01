# ADR-033 (UX/Frontend) — Tela Appointment: Agenda completa

**Status:** Proposto (decisões de UX aceitas; pendente implementação frontend)
**Data:** 2026-06-30
**Autoria:** Carla (UI/UX Agent) + Robson
**Relaciona:** ADR-029 (backend `appointment`), ADR-030 (Home Modo Operação — worklist "A agendar" + Sheet "Agendar").
**Natureza:** decisão de UX/produto. Esta é a master; **spec de UX detalhada (layout, componentes, estados, a11y) no espelho frontend** `B:\projects\odontocore.crm.frontend\docs\adr-frontend-003-appointment-agenda.md` — manter em sincronia.

---

## 1. Contexto
A ADR-030 entregou o **worklist "A agendar"** + **Sheet "Agendar"**. Faltava a metade de execução: **ver e operar o que já está agendado** (agenda do dia). Os dados existem no backend (ADR-029) mas não tinham tela.

## 2. Decisão
- Tela **Agenda** com **2 visões** num segmented (não duas telas): **"Agenda do dia"** (`status=SCHEDULED`, ordenado por `scheduledAt`) e **"A agendar"** (reusa worklist + Sheet da ADR-030).
- **Concluir** = micro-ação inline otimista (`PATCH /complete` responde **202 sem corpo**).
- **Cancelar** = destrutivo, **motivo obrigatório** (`cancelReason` NotBlank) — nunca 1 tap; abre sheet com confirmação.
- **Remarcar / Reatribuir** = sheet (`/reschedule`, `/assignee`).
- Cor por `type` (EVALUATION=laranja, PROCEDURE=esmeralda), **sempre com rótulo textual**.
- Filtro de executor **oculto/travado quando clínica solo** — derivar do usuário logado, não hardcode.
- Conflito de horário = **aviso, não bloqueio** (herda ADR-030; `ConflictWarning` do batch).

## 3. Contrato consumido (código real, 2026-06-30)
`AppointmentResponseDTO` já traz `customerName` e `procedureName` (resolve itens #1/#2 da ADR-030). Status: `AWAITING_SCHEDULE·SCHEDULED·DONE·CANCELLED`. RBAC `APPOINTMENT` só READ/UPDATE; ADM_SYSTEM GLOBAL (solo vê tudo). Detalhe completo de endpoints/DTOs no espelho frontend §3.

## 4. Impactos no backend [IMPACTO BACKEND]
- **[A1]** Não existe "agenda do dia multi-executor": os GET são `?status=` OU `?assignedTo&from&to` (um executor). Falta `GET /appointments?from&to` scope-aware. **MVP solo** usa `?assignedTo={usuárioÚnico}&from&to`.
- **[A2]** `/complete` responde 202 sem corpo → update otimista/refetch no front (sem DTO pra fundir).

## 5. Próximos passos
1. Alinhar [A1] (endpoint por período sem assignedTo) para multi-executor.
2. Implementar `AppointmentRow` + visão Agenda; reusar Sheet "Agendar" da ADR-030.
