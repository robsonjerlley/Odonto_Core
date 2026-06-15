# ADR-016: Bônus mensal vs. métricas por range no user-performance

**Status**: Aceito
**Data**: 2026-06-14
**Autores**: Backend (revisão guiada) — escalado a partir de achado do agente de frontend
**Impacto**: `AnalyticsServiceImpl.getUserPerformance()`, `UserPerformanceResultDTO`,
`getGlobalDashBoard()` (chamador interno), `frontend-integration-contract.md`

---

## Contexto

O `UserPerformanceResultDTO` mistura, no mesmo objeto, métricas com **duas semânticas de
período diferentes**:

- `conversionPct`, `expectedCash`, `avgTicketValue` → calculadas sobre o **range exato** `from..to`
  recebido no request (`AnalyticsServiceImpl` linhas ~210-250).
- `calculatedBonus` → derivado de `periodRef = from.format("yyyy-MM")` e, dentro de
  `getCalculatedBonus()`, o range é **reconstruído como o mês calendário inteiro**
  (`YearMonth.atDay(1) .. atEndOfMonth()`), ignorando o range original.

### Por que o bônus é mensal por design

`BonusConfig` é chaveado por `periodRef` no formato `yyyy-MM` — a regra de percentual de bônus é
configurada **por mês**. Não existe "% de bônus para um range de 10 dias". A reconstrução do mês
em `getCalculatedBonus()` é coerente com esse modelo; o problema não é o bônus ser mensal.

### O problema real

1. **Vazamento de semântica:** um request `from=2026-05-10 & to=2026-05-20` retorna performance
   sobre 10–20/mai mas bônus sobre 01–31/mai, sem o contrato sinalizar a diferença.
2. **Agravante (perda silenciosa de dados):** a derivação usa **apenas o mês do `from`**. Um range
   que cruza meses (`from=2026-05-25 & to=2026-06-05`) computa o bônus só de maio e **descarta junho
   sem aviso**.

O agente de frontend mascarou o item 1 na UI com rótulo honesto ("Bônus de {mês}") e escalou a
inconsistência de origem para o backend.

---

## Decisão

Adotada a opção **(b): tornar o `periodRef` explícito no user-performance**, com guard contra o
agravante.

### 1. `bonusPeriodRef` explícito na resposta

`UserPerformanceResultDTO` ganha o campo `String bonusPeriodRef` (`yyyy-MM`). O consumidor passa a
saber **a qual mês o `calculatedBonus` se refere** — a ambiguidade deixa de ser inferida.

### 2. Guard de mês único

`getUserPerformance()` rejeita ranges que cruzem meses calendário:

```java
if (!YearMonth.from(period.from()).equals(YearMonth.from(period.to()))) {
    throw new IllegalArgumentException(
        "Analytics de performance carrega bônus mensal: o range deve estar contido em um único mês calendário.");
}
```

Mapeado pelo `GlobalExceptionHandler` para **422 Unprocessable Content**. Mata a perda silenciosa:
em vez de descartar um mês sem aviso, o request inválido falha explicitamente.

### Rejeitada — opção (a): calcular o bônus sobre o range exato

Conflita com o domínio: `BonusConfig` é mensal; aplicar a % configurada sobre um range arbitrário
não tem significado de negócio e destruiria a normalização que o `periodRef` faz de propósito.

---

## Consequências

### Positivas
- Contrato honesto: o mês do bônus é explícito, não inferido do `from`.
- Perda silenciosa de dados eliminada (cross-month → 422).
- Sem mudança no modelo de dados (`BonusConfig` permanece mensal).

### Negativas / Riscos
- **`getGlobalDashBoard()` reutiliza `getUserPerformance()`** (montagem de `topPerformers`). O guard
  passa a valer **também para o dashboard global**: consultar o dashboard com range que cruza meses
  retorna 422. Postura consistente ("toda métrica que carrega bônus é mensal — consulte um mês por
  vez"), mas é uma mudança de comportamento do dashboard. Se o produto exigir dashboard multi-mês,
  escopar o guard apenas no entry point `/user-performance/{id}` (controller) em vez do service.
- **Contrato HTTP alterado:** novo campo `bonusPeriodRef` na resposta + novo caso de erro 422.
  Atualizar `frontend-integration-contract.md`.

---

## Referências Cruzadas
- `ADR-015` — analytics scope-aware (define quais métodos cada scope acessa)
- `ADR-007` — BonusResultDTO / config de bônus
- `frontend-integration-contract.md` — §user-performance (atualizar: campo + erro 422)
