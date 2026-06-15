# ADR-017: Dashboard global aceita range livre — desacoplado do guard de bônus mensal

**Status**: Aceito — implementação pendente (próxima sessão)
**Data**: 2026-06-15
**Autores**: Backend (revisão guiada / J.A.R.V.I.S) — materialização de risco previsto em ADR-016
**Impacto**: `AnalyticsServiceImpl.getGlobalDashBoard()`, `AnalyticsServiceImpl.getUserPerformance()`,
método-núcleo novo (privado), `topPerformers` em `GlobalDashBoardResultDTO`

---

## Contexto

A ADR-016 introduziu, em `getUserPerformance()`, um **guard de mês único** (rejeita range que cruza
meses → `IllegalArgumentException` → 422). A própria ADR-016 registrou como **risco conhecido**
(seção "Negativas / Riscos", linhas 80-84) que `getGlobalDashBoard()` reutiliza `getUserPerformance()`
para montar `topPerformers`, e que o guard passaria a valer também para o dashboard.

**O risco se materializou em produção (Railway):**

- Request: `GET /api/v1/analytics/dashboard?from=2026-05-16&to=2026-06-15` (um "últimos 30 dias").
- Resposta: **422 Unprocessable Content** — `"Analytics de performance carrega bônus mensal: o range
  deve estar contido em um único mês calendário."`
- Stacktrace: `getUserPerformance(...:211)` chamado de `lambda$getGlobalDashBoard$23(...:296)` — o
  loop `findByActiveTrue().map(u -> getUserPerformance(u.getId(), period))`.

O range cruza maio/junho, dispara o guard logo no primeiro performer, e o dashboard inteiro falha.

### A pergunta de produto

Um dashboard executivo (visão do `ADM_SYSTEM`) precisa de **range livre** — "últimos 30 dias",
trimestre, etc. Forçar mês calendário no dashboard é limitação de UX inaceitável. **Decisão de
produto: o dashboard global aceita range arbitrário.**

---

## Decisão

Adotada a **Opção B: desacoplar o dashboard do guard via extração de um núcleo de performance
sem bônus.**

### Causa-raiz (não é o guard — é o acoplamento)

`getUserPerformance()` tem **duas responsabilidades de naturezas temporais diferentes** amarradas:

1. **Métricas de performance** (`totalAssigned`, `totalConverted`, `conversionPct`, `avgTicketValue`,
   `expectedCash`) → válidas para **qualquer range**.
2. **Bônus mensal** (`calculatedBonus` + `bonusPeriodRef`) → preso a **um mês calendário**; é a única
   razão de existir do guard.

O dashboard só precisa do item 1, mas, ao reusar o método inteiro, herdou o guard do item 2.

### Estrutura-alvo (3 métodos)

**1. Núcleo novo — `computePerformance(User targetUser, DataRangeDTO period)`** (privado)
- Contém **só** o cálculo de métricas por setor (hoje ~linhas 218-255).
- **Sem** guard de mês, **sem** scope check, **sem** cálculo de bônus.
- Recebe o `User` **já carregado** (não o `UUID`).
- Monta o DTO com `calculatedBonus = ZERO` e `bonusPeriodRef = null` (range não tem mês único; `null`
  é mais honesto que inventar um mês a partir do `from`).

**2. `getUserPerformance(UUID, period)`** (endpoint dedicado — fica magro)
- Mantém: scope check, guard OWN, `findById`, **e o guard de mês único** (linhas 210-213).
- Chama `computePerformance(...)` e **acrescenta** `bonusPeriodRef` + `calculatedBonus`.

**3. `getGlobalDashBoard()`** (ajuste no loop, ~linha 296)
- Troca `.map(u -> getUserPerformance(u.getId(), period))` por `.map(u -> computePerformance(u, period))`.
- O guard de mês **nunca roda** no caminho do dashboard.

```
// Esboço estrutural — NÃO é a implementação final (J.A.R.V.I.S documenta, o dev implementa)

private UserPerformanceResultDTO computePerformance(User targetUser, DataRangeDTO period) {
    // métricas por setor (range livre) → DTO com calculatedBonus=ZERO, bonusPeriodRef=null
}

public UserPerformanceResultDTO getUserPerformance(UUID targetUserId, DataRangeDTO period) {
    // scope check + guard OWN + findById + GUARD DE MÊS ÚNICO (permanece aqui)
    // var base = computePerformance(targetUser, period);
    // calcula bonusPeriodRef + calculatedBonus e devolve o DTO com esses dois campos preenchidos
}

public GlobalDashBoardResultDTO getGlobalDashBoard(DataRangeDTO period) {
    // ...
    // topPerformers = findByActiveTrue().stream().map(u -> computePerformance(u, period)).toList();
}
```

### Sub-decisão de produto

**`topPerformers` no dashboard NÃO exibe bônus** (ranking por métricas de conversão/receita).
Coerente: bônus é mensal e não se aplica a range livre. Se o produto exigir bônus no card, seria
necessário somar bônus mês a mês ao longo do range — rejeitado por ora (YAGNI).

### Por que Opção B e não a alternativa sugerida pela ADR-016

A ADR-016 sugeriu mover o guard para o **entry point do controller** (`/user-performance/{id}`) em
vez do service. Rejeitado: isso **suprime o guard** no caminho do dashboard, mas `getUserPerformance()`
continuaria computando `calculatedBonus` a partir de `from.format("yyyy-MM")` — ou seja, o dashboard
mostraria **bônus só do mês do `from`, descartando o resto silenciosamente** (exatamente a perda
silenciosa que a ADR-016 quis matar). A extração remove o cálculo de bônus do caminho do dashboard
**de vez**, em vez de só esconder o guard.

---

## Consequências

### Positivas
- Dashboard global aceita range livre ("últimos 30 dias", trimestre) sem 422.
- Single Responsibility: guard + bônus ficam só no caminho que produz bônus.
- **Ganho de eficiência:** núcleo recebe `User` já carregado → elimina um `findById` por performer
  (N+1 escondido) **e** o scope check redundante que rodava por usuário no loop.
- Endpoint `/user-performance/{id}` permanece com guard + `bonusPeriodRef` (ADR-016 intacta).

### Negativas / Riscos
- `topPerformers` passa a vir com `calculatedBonus = 0` e `bonusPeriodRef = null` — o frontend não
  deve renderizar bônus nesse card. Atualizar `frontend-integration-contract.md` (§dashboard).
- Mesmo DTO (`UserPerformanceResultDTO`) usado com duas semânticas (com/sem bônus). Aceitável por ora;
  se virar fonte de confusão, considerar DTO próprio enxuto para o ranking do dashboard.

---

## Decisões em aberto para a próxima sessão (implementação)

1. **Núcleo recebe `User` (não `UUID`)** — recomendado (resolve o N+1). _Confirmar._
2. **Injeção do bônus no método 2:** reconstruir o record copiando campos (rápido) **ou** núcleo
   devolve um holder só com os números e cada chamador monta seu DTO (mais limpo). _Escolher._

---

## Referências Cruzadas
- `ADR-016` — bônus mensal vs. métricas por range (origem do guard; este ADR resolve o risco previsto)
- `ADR-015` — analytics scope-aware (define acesso GLOBAL ao dashboard)
- `frontend-integration-contract.md` — §dashboard (atualizar: topPerformers sem bônus)
