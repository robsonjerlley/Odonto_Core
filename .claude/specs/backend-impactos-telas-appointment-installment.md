# Impactos de Backend — Telas Appointment (Agenda) + Installment (Gestão) + Home reversível

**Status:** Aberto — pendente desenho de contrato pelo backend/arquiteto
**Data:** 2026-06-30
**Origem:** ADR-033 (agenda), ADR-034 (gestão de parcelas), ADR-030 rev. 2026-06-30 (home reversível)
**Consumidor:** agente frontend (specs em `odontocore.crm.frontend/docs/adr-frontend-002/003/004`)

> Consolidação dos `[IMPACTO BACKEND]` das specs de UX. Cada item lista: o que o frontend precisa,
> o estado atual do backend (código real, 2026-06-30) e a ação sugerida. Ordenados por bloqueio.
> Nada aqui é decisão fechada de backend — é a lista de lacunas a resolver.

---

## Prioridade 1 — Bloqueiam funcionalidade prometida na UX

### B1 · Persistência de preferência de home (`homeMode`)
- **Precisa:** guardar por usuário `homeMode: AUTO | OPERATION | CARDS` (default `AUTO`), lido no login e alterável.
- **Hoje:** não existe. O `auth` expõe role/sector no JWT; não há store de preferências por usuário.
- **Ação sugerida:** endpoint de preferência do usuário — ex.: `GET /api/v1/users/me/preferences` e `PATCH /api/v1/users/me/preferences {homeMode}`. Persistir em `identity_db`. Resolve **parcialmente o item #10 da ADR-030** (detecção de "modo solo").
- **Nota de produto:** a heurística de default `AUTO` (papel amplo? nº de usuários da clínica = 1?) ainda precisa de critério — ver item #10 da ADR-030 §6.

### B2 · Agenda do dia multi-executor (`[A1]` da ADR-033)
- **Precisa:** listar os agendamentos de um período (dia) de **todos os executores**, scope-aware.
- **Hoje:** `GET /appointments` só aceita `?status=` (sem filtro de data) **ou** `?assignedTo&from&to` (exige UM executor). Não há "todos os executores no intervalo".
- **Ação sugerida:** `GET /api/v1/appointments?from&to` (opcionalmente `&status=SCHEDULED`), scope-aware, sem exigir `assignedTo`.
- **Workaround MVP solo:** usar `?assignedTo={idDoUsuárioLogado}&from&to`. Válido só enquanto a clínica tem 1 executor — não hardcodar a suposição.

### B3 · Parcelas atrasadas cross-month (`[I2]` da ADR-034)
- **Precisa:** listar parcelas **atrasadas de qualquer mês** num único request — para o feed "Pagamentos pendentes" da Home (ADR-030 §4.5) e para um chip "atrasados" global.
- **Hoje:** `GET /installments` exige `?month=yyyy-MM`. Não há consulta cross-month; "atrasado" nem é status (ver B5).
- **Ação sugerida:** `GET /api/v1/installments?overdue=true` (sem `month`), scope-aware, paginado — retorna todas as `EXPECTED` com `dueDate < hoje`.

---

## Prioridade 2 — Contornáveis no MVP, mas geram dívida

### B4 · Pagamento parcial de parcela (`[I3]` da ADR-034)
- **Precisa:** registrar recebimento **parcial** (paguei R$200 de uma parcela de R$300) mantendo saldo.
- **Hoje:** `PaymentStatus` só tem `EXPECTED | PAID`. `PATCH /{id}/pay` marca **PAID** mesmo se `paidAmount < expectedAmount` — sem saldo residual.
- **Ação sugerida (futuro):** status `PARTIAL` + campo de saldo, ou parcela-filha do restante. **Decisão do P.O.** se entra no MVP.
- **Contorno MVP:** o front **avisa** que pagamento parcial quita a parcela sem guardar saldo (já especificado na ADR-034 §3).

### B5 · "Atrasado" como faceta, não status (`[I1]` da ADR-034)
- **Precisa:** filtrar/contar atrasados no servidor.
- **Hoje:** "atrasado" é o boolean **derivado** `overdue` no `InstallmentResponseDTO` — não é valor de `PaymentStatus`, então **não é filtrável** por `?status=`.
- **Ação sugerida:** ou manter derivado e o front filtra client-side (aceitável dentro de um mês já carregado), ou expor `?overdue=true` (casa com B3). **Não** criar um `PaymentStatus.OVERDUE** (atrasado é função do tempo, não estado persistido).

---

## Observações de contrato (não exigem mudança, mas o front precisa saber)

### B6 · `PATCH /appointments/{id}/complete` responde `202` sem corpo (`[A2]`)
- Sem DTO atualizado para fundir no estado → o frontend faz **update otimista** (linha → `DONE`) e/ou refetch. Mesmo padrão em `PATCH /installments/{id}/unpay` (`200` sem corpo).

### B7 · Datas são horário de Brasília naïve (ADR-009)
- `LocalDateTime`/`LocalDate` chegam sem offset e **representam Brasília**. Não converter UTC→local. Vale para `scheduledAt`, `dueDate`, `paidAt`.

---

## Resumo para priorização

| # | Item | Bloqueia | Esforço estimado | Depende de decisão |
|---|------|----------|------------------|--------------------|
| B1 | Preferência `homeMode` | Home reversível | Baixo (CRUD simples) | critério do default AUTO (produto) |
| B2 | Agenda multi-executor | Agenda p/ clínica >1 executor | Baixo (query nova) | — |
| B3 | Atrasados cross-month | Feed home + chip global | Baixo (query nova) | casa com B5 |
| B4 | Pagamento parcial | Precisão financeira | Médio (modelo) | P.O. (entra no MVP?) |
| B5 | Filtro `overdue` server | Filtro global de atrasados | Baixo | casa com B3 |

> **Próximo passo:** levar B1–B3 e B5 ao arquiteto para desenho dos contratos REST (são queries/endpoints simples). B4 é decisão de produto antes de virar contrato.
