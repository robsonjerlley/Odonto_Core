# ADR-030 (UX/Frontend) — Scheduling: Home "Modo Operação" + Sheet "Agendar"

**Status:** Proposto (decisões de UX aceitas; pendente implementação)
**Data:** 2026-06-27
**Autoria:** Carla (UI/UX Agent) + Robson
**Relaciona:** ADR-029 (módulo `appointment`), ADR-031 (`Deal.paymentStatus` — feed §4 "Pagamentos pendentes"). ⚠️ ADR-023 (TicketWonEvent) foi **substituída pela ADR-029** — o gatilho real é o `DealWonEvent` síncrono, não o evento async da 023
**Natureza:** decisão de UX/produto com **impactos no backend** (ver seção 6). Esta é a master; cópia espelhada no frontend em `B:\projects\odontocore.crm.frontend\docs\adr-frontend-002-scheduling-home-modo-operacao.md` — manter as duas em sincronia.

> ⚠️ **Não presumir o formato do backend.** Onde há dependência de dado novo ou endpoint que talvez não exista, está marcado **[IMPACTO BACKEND]**. Confirmar contra os fontes antes de implementar; se o contrato não existir, alinhar back/front.

---

## 1. Problema de produto

O sistema cresce em número de telas (funil, pacientes, avaliações, negociações, agora agenda; financeiro virá). A `HomePage` atual é um **lançador de cards** (menu), não um cockpit. A navegação é filtrada por papel — mas em clínica pequena **um único usuário acumula todos os papéis**, então vê todos os ~10 atalhos e a home não diz "o que fazer agora".

Risco estratégico: a fatia de mercado de **clínicas pequenas** (1–2 pessoas) é perdida por exaustão operacional (saltar entre telas + redigitar dados).

## 2. Personas e contextos de uso

| Persona | Contexto | Necessidade |
|---|---|---|
| **Dona-faz-tudo** (solo, todos os papéis) | clínica pequena | resolver o dia sem caçar menus |
| **Time** (papéis separados) | clínica média | a home de cards atual já serve |

**Dois dispositivos, dois jobs (igualmente importantes):**
- **Mobile — antes da clínica (planejamento):** "como vai ser meu dia?" → LER muito, digitar pouco.
- **Desktop — dentro da clínica (execução):** "próximo paciente, executa, marca" → AGIR muito, formulários.

## 3. Decisão central

Home **adaptativa ao modo de operação**, não ao módulo:
- Solo / papel amplo → **Home "Modo Operação"** (feed de ação).
- Time → mantém o grid de cards atual.
- **Sem toggle.** O perfil define a home; a sidebar segue dando acesso aos módulos para o detalhe.

> A "Home Modo Operação" **não é um dashboard** (gráficos) nem um **módulo novo**. É uma tela que *consome* dados já existentes (funil, avaliações, deals) + os novos (agenda, a-agendar, status de pagamento) e os apresenta como **tarefas em ordem de ação**, com **micro-ações inline** para não navegar a cada item.

## 4. Home "Modo Operação" — especificação

### Hierarquia (ordem por urgência do dia)
1. **Resumo do dia** (1 linha: "X atrasados · Y tarefas hoje")
2. **⚠️ Atrasado** — lead sem contato, atendimento sem desfecho (vermelho + rótulo textual)
3. **📅 Hoje** — agenda do dia (avaliações + procedimentos), ordem de horário
4. **🗓️ A agendar** — worklist do WIN (procedimentos fechados sem data)
5. **💰 Pagamentos pendentes** — status, não módulo

### Layout
Feed de **coluna única**, largura controlada. Cada seção: header (ícone + rótulo + contador + "ver tudo →") e N linhas (`TaskRow`). Reusar os acentos de cor já existentes por módulo (azul=funil, laranja=avaliação, esmeralda=deal, etc.).

### Responsividade (conteúdo adaptativo, não só reflow)
| | Mobile (< 768px) — planejar | Desktop (≥ 768px) — executar |
|---|---|---|
| Resumo do topo | protagonista | linha discreta |
| Seção "Hoje" | expandida (herói) | expandida |
| Demais seções | recolhidas (chip + contador, tap p/ abrir) | expandidas |
| Ações | swipe na linha + sheet | botões inline visíveis |
| Densidade | enxuta | completa |

### Componente `TaskRow`
```
Estados:
- padrão: ícone(cor do módulo) · título · subtítulo(contexto) · ações
- hover (desktop): realça fundo
- swipe (mobile, ←): revela 1–2 ações; >50% confirma a primária
- carregando ação: botão → spinner
- concluído: fade-out 200ms + contador da seção decrementa
Responsividade:
- < 768px: ações ocultas; tap = sheet de detalhe; swipe = ação rápida
- ≥ 768px: ações inline; tap na linha = sheet só se precisar form
Acessibilidade:
- alvo de toque ≥ 44×44px; contraste ≥ 4.5:1
- urgência com rótulo textual ("Atrasado"), nunca só cor
- toda ação por swipe tem equivalente por tap
- foco por teclado na linha; Enter = ação primária; Esc fecha sheet
```

### Estados da Home
- **Vazio (dia limpo):** "Tudo em dia ✨" + atalho discreto p/ o funil.
- **Seção vazia:** não renderizar (sem "(0)").
- **Carregando:** skeleton 2–3 linhas por seção.
- **Erro isolado:** falha de uma seção não derruba o feed.
- **Offline/lento (mobile no trânsito):** último cache com selo "atualizado às HH:MM".

### Micro-ações (deep-link só quando precisa de form)
- 1 clique no card: ligar, concluir atendimento, marcar pago.
- Abre sheet/modal: agendar, remarcar (precisam de input).

## 5. Sheet "Agendar" — especificação

**Gatilho:** ação "Agendar" numa linha de "A agendar" (appointment em `AWAITING_SCHEDULE`).
**Tipo:** side sheet (desktop) / bottom sheet (mobile).
**Objetivo:** `AWAITING_SCHEDULE → SCHEDULED` definindo data/hora.

**Princípio "digite uma vez":** dados conhecidos vêm preenchidos e travados; o usuário só insere o que é novo.

```
Read-only (snapshot):
  Paciente       (deal → ticket → customer)
  Procedimento + duração estimada (snapshot + catálogo)
  Plano: "Sessão X de N" (DealProcedure.quantity)

Inputs:
  Data e hora *           ← único obrigatório
  Profissional (executor) ← default = evaluator; dropdown; grava assignedTo
  Anotação (opcional)
  ☐ Planejar as N sessões de uma vez → a cada [k] dias, mesmo horário (opt-in)

Calculado / feedback:
  "Termina HH:MM · N min" (de estimatedDuration)
  Conflito inline se o executor já tiver atendimento na janela (aviso, não bloqueio)
```

**Decisões:**
- Default = agendar **só a sessão 1**. "Planejar N de uma vez" é opt-in → botão vira "Agendar N sessões" + lista de datas propostas **editáveis** antes de confirmar.
- Conflito = **warning, não bloqueio** (clínica encaixa).
- `estimatedDuration` nulo → "duração não definida" + campo manual; não trava.

**Estados:** validando · conflito · data no passado (erro inline) · salvando · sucesso (fecha, item sai da worklist, toast) · erro (mantém dados, msg humana).
**A11y:** labels visíveis; foco inicial no campo de data; `Esc` fecha (confirma se houver edição).

## 6. Necessidades de dados / impactos no backend  ⚠️ NÃO PRESUMIR

> ✅ **Atualização (2026-06-28):** os 10 `[IMPACTO BACKEND]` abaixo foram **fechados na ADR-029**
> (contratos REST do `AppointmentController`, worklist com nomes via snapshot, batch `schedule-batch`,
> RBAC `Resource.APPOINTMENT`). O item #9 (status de pagamento) virou a **ADR-031** (`Deal.paymentStatus`);
> o #10 (perfil solo) é Travamento D da ADR-029 (atributo no onboarding). A tabela permanece como
> registro da origem das necessidades — o contrato vigente está na ADR-029, não aqui.

| # | Necessidade do frontend | Provável estado no backend | Ação |
|---|---|---|---|
| 1 | Worklist "A agendar": `appointmentId, paciente(nome), procedimento(nome), duraçãoEstimada, sessão X/N, executorPadrão, dealId, ticketId` | Appointment nasce no WIN (ADR-029); **listagem da worklist não existe** | **[IMPACTO BACKEND]** `GET /appointments?status=AWAITING_SCHEDULE` (scope-aware) com nomes resolvidos |
| 2 | Nomes de paciente/procedimento prontos p/ exibir | Appointment guarda IDs; nomes via snapshot do deal | **[IMPACTO BACKEND]** resposta já trazer nomes (preferível) ou front resolve |
| 3 | Agenda do dia por executor + período | scope-aware previsto (ADR-013/015); **endpoint a definir** | **[IMPACTO BACKEND]** `GET /appointments?assignedTo&from&to` |
| 4 | Conflito de horário do executor | não existe | **[IMPACTO BACKEND]** endpoint de disponibilidade OU front deriva da agenda já carregada (preferir derivar) |
| 5 | Agendar 1 sessão (data/hora + assignedTo) | previsto; contrato não definido | **[IMPACTO BACKEND]** `PATCH /appointments/{id}/schedule` |
| 6 | Reatribuir executor | previsto (assignedTo mutável) | **[IMPACTO BACKEND]** `PATCH /appointments/{id}/assignee` |
| 7 | Remarcar / cancelar / concluir | máquina `AWAITING_SCHEDULE → SCHEDULED → DONE \| CANCELLED` | **[IMPACTO BACKEND]** endpoints + `cancelReason` obrigatório |
| 8 | "Planejar N sessões" (batch + recorrência) | não existe | **[IMPACTO BACKEND]** endpoint batch OU front faz N chamadas (batch melhor p/ consistência) |
| 9 | Status de pagamento (pago/pendente) | **financeiro não existe**; P.O.: só status, não tela | **[IMPACTO BACKEND]** campo de status mínimo, não ERP |
| 10 | Detecção do perfil "solo/papel amplo" p/ escolher a home | role/sector no `auth`; "clínica tem 1 usuário" não exposto | **[IMPACTO BACKEND/REGRA]** definir critério (papel amplo? nº de usuários?) e origem |

## 7. Decisões registradas (resumo)

- Home adaptativa por **modo de operação**; feed de ação p/ solo-operador; **sem toggle**.
- Mobile = **planejar** (ler), Desktop = **executar** (agir) — conteúdo adaptativo.
- Micro-ações inline; sheet/modal só para input.
- Sheet "Agendar" com "digite uma vez": só data/hora obrigatória; default 1 sessão, batch opt-in.
- Conflito de horário = aviso, não bloqueio.
- Financeiro no feed = **status pago/pendente**, não módulo (MoSCoW: Won't-have a tela completa agora).

## 8. Próximos passos

1. Alinhar os **[IMPACTO BACKEND]** da seção 6 (contratos REST) — refletir na ADR-029.
2. Spec visual fina (tokens, breakpoints, microcopy) quando os contratos fecharem.
3. Implementar `TaskRow` + Home Modo Operação; depois o Sheet "Agendar".
