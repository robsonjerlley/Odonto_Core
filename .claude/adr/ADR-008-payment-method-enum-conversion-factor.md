# ADR-008: PaymentMethod enum com conversionFactor para métricas de caixa

**Status**: Aceito  
**Data**: 2026-06-02  
**Autores**: Arquiteto-Agent  
**Impacto**: `core/enums`, `commercial/model/Deal`, `commercial/api/dto/request/deal/CloseDealRequestDTO`, `commercial/api/dto/response/deal/DealResponseDTO`, `commercial/service/DealService`, `commercial/service/impl/DealServiceImpl`

---

## Contexto

### Problema identificado no frontend

`Deal.paymentMethod` era `String` livre. O frontend definia os valores possíveis mas o backend não validava — qualquer string era aceita. Isso criava dois riscos concretos:

1. **Inconsistência de dados**: `closeDeal` chamado com strings variantes do mesmo conceito gerava registros distintos
2. **Analytics frágil**: métricas de vendas dependem de agrupamento por forma de pagamento — agrupamento sobre strings livres é inutilizável

### Por que não é dado do módulo financeiro

`Deal.paymentMethod` não é registro de transação financeira. É o **acordo de pagamento no momento do fechamento** — dado CRM, não financeiro. Quando o módulo financeiro entrar, ele terá sua própria entidade `PaymentTransaction` com parcelas, datas de vencimento e valores liquidados. O `paymentMethod` no Deal serve exclusivamente como **fator de ponderação de meta comercial**: permite ao gestor calcular expectativa real de caixa sobre os fechamentos para tomar decisões de bonificação.

Exemplo concreto: R$10.000 parcelado não equivale a R$10.000 em PIX para o caixa esperado do mês. A bonificação do vendedor deve refletir isso.

---

## Decisão

### `PaymentMethod` enum em `core/enums/` com `conversionFactor`

Cada valor carrega um fator de conversão (`BigDecimal`) representando a expectativa de realização em caixa, considerando risco de inadimplência e taxas operacionais aproximadas.

| Enum | Label frontend | `conversionFactor` | Justificativa |
|---|---|---|---|
| `PIX` | "Pix" | 1.00 | Liquidação imediata, sem taxa |
| `CASH` | "Dinheiro" | 1.00 | Recebimento físico imediato |
| `DEBIT_CARD` | "Cartão de Débito" | 0.98 | Taxa média de maquininha ~2% |
| `CREDIT_CARD` | "Cartão de Crédito" | 0.97 | Taxa média de maquininha ~3% |
| `INSTALLMENT` | "Parcelado" | 0.85 | Risco de inadimplência em parcelas futuras |
| `DENTAL_INSURANCE` | "Convênio" | 0.90 | Repasse parcial do convênio + prazo de liquidação |

**Os fatores são aproximações de negócio, não valores exatos.** Clínicas com histórico diferente podem ajustar via código — a complexidade de uma tabela configurável não se justifica no horizonte do MVP.

### Valores excluídos e justificativa

- **Transferência bancária**: removida — no contexto atual (PIX disponível), transferência TED/DOC não tem caso de uso distinto de PIX para clínicas odontológicas
- **Boleto**: substituído por `INSTALLMENT` — o conceito relevante para a clínica é parcelamento, não o instrumento de cobrança
- **Financiamento**: substituído por `DENTAL_INSURANCE` (Convênio) — o financiamento externo via financeiras não é prática comum nesse segmento; convênio odontológico sim

### Impacto no Analytics

`getGlobalDashboard` e `getUserPerformance` passam a calcular:

```
expectedCash = finalValue × paymentMethod.getConversionFactor()
```

O gestor visualiza dois números distintos: **volume bruto fechado** e **expectativa real de caixa** — base para decisão de bonificações.

---

## Arquivos alterados

```
NOVO (1 arquivo)
└── core/enums/PaymentMethod.java

MODIFICADOS (5 arquivos)
├── commercial/model/Deal.java                                  (String → PaymentMethod + @Enumerated)
├── commercial/api/dto/request/deal/CloseDealRequestDTO.java    (@NotBlank String → @NotNull PaymentMethod)
├── commercial/api/dto/response/deal/DealResponseDTO.java       (String → PaymentMethod)
├── commercial/service/DealService.java                         (assinatura closeDeal)
└── commercial/service/impl/DealServiceImpl.java               (assinatura closeDeal)

BANCO DE DADOS
└── Nenhuma migration necessária — coluna já existe como VARCHAR;
    Hibernate valida o valor via @Enumerated(STRING) na escrita.
    Feature não estava em produção — sem dados históricos inválidos.
```

---

## Por que `@NotNull` e não `@NotBlank`

Com `String`, `@NotBlank` era o único guarda contra campo vazio. Com enum tipado, o Jackson falha na desserialização se o valor não existir no enum — HTTP 400 automático antes de chegar na camada de validação. `@NotNull` garante que o campo foi enviado; o tipo garante que o valor é válido.

---

## Consequências Positivas

- Contrato explícito: qualquer cliente que chamar `closeDeal` com valor inválido recebe 400 imediatamente
- Analytics pode calcular `expectedCash` de forma confiável sem parsing de string
- Enum em `core/enums/` fica disponível para o módulo financeiro referenciar o acordo original sem duplicar conceito
- Espelha o padrão já adotado por `ContactChannel` e `AdsChannel`

## Consequências Negativas / Riscos

- Fatores de conversão fixos no código: ajuste requer deploy. Aceitável no MVP — comportamento de inadimplência de uma clínica não muda com frequência
- Enum coarse-grained: `INSTALLMENT` não captura número de parcelas. O módulo financeiro, quando implementado, terá esse detalhe em `PaymentTransaction` — não é responsabilidade do CRM

---

## Alternativas Consideradas

- **Manter `String` como débito técnico consciente**: descartado. O módulo analytics já está entrando — agrupamento por string livre tornaria as métricas de pagamento inutilizáveis desde o primeiro deploy
- **Tabela `PaymentMethodConfig` com fatores configuráveis**: descartado. Adiciona entidade, repositório, endpoint e UI para um valor que muda raramente. Pode ser adicionado em fase futura se a clínica precisar calibrar os fatores
- **`conversionFactor` como campo em `Deal`**: descartado. Requer disciplina operacional do vendedor e adiciona campo à UI de fechamento sem ganho de precisão relevante

---

## Referências Cruzadas

- `ContactChannel`, `AdsChannel` — padrão de enum sem atributo; `PaymentMethod` estende o padrão adicionando `conversionFactor` como atributo de negócio
- `ADR-007` — padrão de enums globais em `core/enums/`; `PaymentMethod` segue o mesmo pacote