# ADR-021 — AdsInvestment: query de overlap para cálculo de ROI

**Status:** Implementado  
**Data:** 2026-06-17  
**Afeta:** `AdsInvestmentRepository`, `ConfigServiceImpl`, `AnalyticsServiceImpl`

---

## Contexto

`AdsInvestment` armazena o investimento de uma campanha de mídia paga com um período
de vigência (`periodStart`, `periodEnd`). O cálculo de ROI em `getAdsROI(channel, period)`
precisa somar todos os investimentos do canal que **estiveram ativos** dentro do período
consultado.

A query original usava containment:

```sql
periodStart >= :from AND periodEnd <= :to
```

Isso exige que a campanha esteja inteiramente contida dentro do período. Na prática,
qualquer campanha que começasse antes ou terminasse depois do período consultado era
ignorada completamente, subavaliando (ou zerando) o `totalInvestment` do ROI.

**Exemplo do problema:**  
Campanha META: Jan 1 – Mar 31, R$ 9.000. Consulta: Fev 1 – Fev 28.  
Com containment: **excluída** (Jan 1 não é ≥ Fev 1).  
ROI calculado com investimento = 0 → divisão por zero ou ROI inflado infinitamente.

---

## Decisão

Substituir por **overlap**:

```sql
periodStart <= :to AND periodEnd >= :from
```

Dois intervalos `[A,B]` e `[C,D]` se sobrepõem quando `A ≤ D AND B ≥ C` — condição
matemática padrão de interseção de intervalos. Inclui toda campanha que esteve ativa
em qualquer dia do período consultado.

---

## Consequências

**Positivas:**
- ROI de ADS passa a refletir o investimento real do período, incluindo campanhas multi-mês.
- Elimina o risco de divisão por zero por `totalInvestment = 0` em consultas válidas.
- O módulo financeiro (quando implementado) receberá dados corretos de custo por canal.

**Trade-off aceito:**
- Campanhas multi-mês entram com o `amount` total, sem prorateamento por dia.  
  Ex: campanha Jan–Mar (R$ 9.000) consultada em Fev retorna R$ 9.000, não R$ 3.000.  
  Esse comportamento é intencional: `AdsInvestment.amount` representa o orçamento
  total da campanha, não um custo diário. Prorateamento exigiria mudança de modelo
  de dados (fora de escopo neste momento).

**Arquivos alterados:**
- `modules/commercial/repository/AdsInvestmentRepository.java` — query corrigida
- `src/main/resources/refactors/odontocore_class_diagram.html` — nota de overlap adicionada
- `src/main/resources/refactors/odontocore_arquitetura_definitiva.html` — métodos reais corrigidos + nota

---

## Impacto no módulo financeiro

Quando o módulo financeiro for implementado, qualquer consulta de custo de campanha
por período **deve usar a condição de overlap** acima, nunca containment. O padrão
correto está encapsulado em `ConfigService.sumInvestmentByChannelAndPeriod()`.
