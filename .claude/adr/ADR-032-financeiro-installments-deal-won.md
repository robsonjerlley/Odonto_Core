# ADR-032: Módulo `financeiro` — parcelas a receber (`Installment`) a partir do Deal fechado

**Status**: Proposto (decisão fechada em 2026-06-30; pendente implementação)
**Data**: 2026-06-30
**Autores**: P.O.-Agent + Arquiteto-Agent + Robson
**Impacto**: novo módulo `financeiro` (crm_db); `commercial` (`Deal.installmentCount` + `DealFinancialProvider`); `analytics` (`recebido`/`aReceber` derivados de fato real)
**Relaciona**: **substitui a ADR-031** (`Deal.paymentStatus` binário — a decisão evoluiu de flag-no-Deal para módulo-via-evento); ADR-029 (mesmo gatilho `DealWonEvent`, mesmo padrão de filhos-chapados por deal); ADR-028 (read-boundary `Provider`/`View`); ADR-024 (`@TenantId`); ADR-027 (sem migration — `ddl-auto`)

> Esta ADR **supera a ADR-031** como a ADR-029 superou a ADR-023. A 031 cortou parcelamento para "fase 2" e reduziu o financeiro a um flag `PAID/PENDING` por deal — recorte que resolvia o problema errado: o Analytics já projeta `totalExpectedCash` cego, então um binário por-deal não agrega valor real. A dor concreta do público-alvo (clínica de gestor único) é **rastrear parcelas mês a mês** — a maioria dos procedimentos é parcelada. Sem isso, o gestor busca cliente por nome e conta parcela na mão: inviável. O parcelamento é o **core value**, não evolução.

---

> # 🎯 Direcionamento de Implementação — leia isto para codar
>
> Módulo **`financeiro`** (`modules/financeiro`) — agenda de **parcelas a receber**. É o **2º consumidor do `DealWonEvent`** (o 1º é `appointment`), com a **mesma forma** que o appointment: `Deal WON` → N filhos chapados por deal.
>
> ```
> Deal WON ──DealWonEvent──┬──▶ appointment: N Appointment   (session_index 1..N)
>                          └──▶ financeiro:  N Installment    (sequence 1..N)
> ```
>
> **Modelo (essencial):**
> - 1 `Installment` = 1 parcela (1 vencimento). `Deal.installmentCount = N` → **N installments** (`sequence 1..N`, `total_installments = N`). **À vista = N=1** (o modelo engloba o binário).
> - Estados: `EXPECTED → PAID`. **`OVERDUE` é derivado no read** (`due_date < hoje && EXPECTED`) — **sem coluna, sem job** no MVP.
> - `expected_amount = finalValue / N` (HALF_UP); **última parcela absorve o resto** (`total − Σ anteriores`) p/ fechar no centavo.
> - `due_date`: parcela `i` vence `closedAt + (i−1) meses` (1ª no fechamento; convenção configurável depois).
> - Snapshot no WIN: `customer_name`, `expected_amount` (não re-resolve no read — feed é single-table).
>
> **Gatilho:** `DealWonEvent` (INALTERADO — appointment já consome e está testado). `InstallmentEventListener` (`@EventListener` **síncrono / mesma transação** do `closeDeal`, fail-fast; NÃO `@Async`, NÃO `AFTER_COMMIT`). Throw → rollback do `closeDeal`. Tenant já no contexto → `@TenantId` preenche sozinho.
>
> **Dado financeiro NÃO vem do evento** — vem do **`DealFinancialProvider`** (read-boundary no `commercial`, padrão ADR-028). O evento traz `dealId`; o listener puxa `DealFinancialView`. Isso mantém a **regra de preço encapsulada no `commercial`** (`expected = finalValue ?? totalValue`) e o `DealWonEvent` intacto.
>
> **Checklist:**
> 1. `core/enums/InstallmentStatus.java` — `EXPECTED, PAID` (só estados persistidos; OVERDUE é flag derivada no DTO).
> 2. `core/enums/Resource.java` — add `INSTALLMENT`.
> 3. `commercial/model/Deal.java` — add `installmentCount` (Integer, default 1).
> 4. `commercial/api/dto/request/deal/CloseDealRequestDTO.java` — add `installmentCount` (`@Min(1)`, default 1).
> 5. `commercial/provider/DealFinancialProvider.java` + `DealFinancialView` — NOVO read-boundary.
> 6. `financeiro/domain/model/Installment.java` — `@Entity` + `@TenantId`, índices `(clinic_id, due_date, status)` e `(clinic_id, customer_id)`. Nasce do entity (`ddl-auto`) — sem migration.
> 7. `financeiro/event/InstallmentEventListener.java` — síncrono; materializa o schedule.
> 8. `financeiro/repository` + Specifications scope-aware (ADR-013) + `financeiro/service` + `InstallmentController`.
> 9. RBAC: seed `PermissionRule` p/ `Resource.INSTALLMENT` (tabela na seção RBAC).
> 10. `analytics` — desdobrar em `recebido`/`aReceber` lendo `Installment` (ver "Impacto no Analytics").
>
> **Fronteira:** `financeiro` só CONSOME — depende de `core/events` (evento) + `commercial` (provider). **Nunca importa `funnel`**; **nunca injeta `DealRepository`**.

---

## Contexto

A ADR-031 modelou um `Deal.paymentStatus` binário para alimentar o feed "Pagamentos pendentes" (ADR-030 §4) e desdobrar o `totalExpectedCash` do Analytics. Ao revisar com o caso real — **gestor único** acumulando captação + avaliação + comercial + financeiro, com ~20 clientes em aberto — ficou claro que:

- O binário `PAID/PENDING` por deal **não resolve a dor**: o Analytics (`AnalyticsServiceImpl:265`) já soma `finalValue × conversionFactor` de todo deal fechado — projeção cega. Trocar projeção por um flag agrega pouco.
- **A maioria dos procedimentos é parcelada** (realidade de mercado odontológico). O fato financeiro não é "deal pago/não-pago" — é "parcela 8 de 10, vencendo no mês 8".
- Sem o sistema materializar isso, o gestor **conta parcela na mão** buscando cliente por nome. Inviável → ele abandona o controle.

Logo, o parcelamento **é o MVP**, não a fase 2.

---

## Decisão

Novo módulo `modules/financeiro` (crm_db), agregado **`Installment`** (parcela), grão por-parcela.

1. **Nasce no WIN** via 2ª escuta do `DealWonEvent` (`InstallmentEventListener`, síncrono, mesma transação — fail-fast, igual appointment). `DealWonEvent` **inalterado**.
2. **Dado financeiro via `DealFinancialProvider`** (read-boundary no `commercial`): o listener tem `dealId` (do evento) e puxa `{ expectedAmount, paymentMethod, installmentCount, closedAt }`. A regra `expected = finalValue ?? totalValue` mora no provider (domínio do `commercial`).
3. **Schedule:** `Deal.installmentCount = N` → N installments `EXPECTED`; `expected_amount = finalValue/N` (HALF_UP, última absorve resto); `due_date` mensal a partir de `closedAt`.
4. **Baixa por parcela:** `PATCH /installments/{id}/pay` → `PAID` + `paid_at` + `paid_amount`. Reversível (`/unpay`).
5. **Analytics** deriva `recebido` (Σ PAID) / `aReceber` (Σ EXPECTED) lendo `Installment` — preciso, por mês.

### Termos do parcelamento — Plano A (capturados na venda)
`installmentCount` é fato da negociação → mora no `Deal`, capturado no `closeDeal` (default 1 = à vista). Descartado o "plano definido manualmente no financeiro pós-WIN": reintroduziria exatamente o trabalho manual que a feature elimina.

---

## Modelo de dados — `crm_db.installments`

```
id              : UUID        PK
clinic_id       : UUID        NOT NULL — @TenantId (ADR-024)
deal_id         : UUID        NOT NULL — origem (Deal won)
customer_id     : UUID        NOT NULL — deep-link ao cadastro canônico
customer_name   : VARCHAR     NOT NULL — snapshot no WIN (feed single-table)
sequence        : INTEGER     NOT NULL — "X" em "parcela X de N"
total_installments: INTEGER   NOT NULL — "N"
due_date        : DATE        NOT NULL — vencimento (closedAt + (sequence-1) meses)
expected_amount : NUMERIC(15,2) NOT NULL — valor da parcela (snapshot)
status          : VARCHAR     NOT NULL — EXPECTED | PAID   (OVERDUE é derivado, não persistido)
paid_amount     : NUMERIC(15,2) nullable — preenchido na baixa (= expected_amount no MVP)
paid_at         : TIMESTAMP   nullable
paid_by         : UUID        nullable
created_at      : TIMESTAMP   NOT NULL
updated_at      : TIMESTAMP   NOT NULL
índices: (clinic_id, due_date, status)   ← "a receber no mês" / caixa futuro
         (clinic_id, customer_id)         ← extrato do cliente
```

- **Sem migration** (ADR-027): a tabela nasce do `@Entity Installment` (`ddl-auto=update`); índices via `@Table(indexes=…)`.
- **À vista** = 1 installment (`sequence=1, total=1, due_date=closedAt`).
- `OVERDUE` = computado no read (`due_date < hoje && status=EXPECTED`) e exposto como flag `overdue` no DTO de resposta — **sem `@Scheduled`**.

### Contrato `DealFinancialProvider` (commercial — read-boundary ADR-028)
```java
public interface DealFinancialProvider {
    DealFinancialView resolveById(UUID dealId);
}
public record DealFinancialView(
    UUID dealId,
    BigDecimal expectedAmount,      // finalValue != null ? finalValue : totalValue  ← regra no commercial
    PaymentMethod paymentMethod,
    int installmentCount,           // Deal.installmentCount (default 1)
    LocalDateTime closedAt
) {}
```
O `financeiro` aplica a regra de **schedule** (split + due dates) sobre esses fatos — a regra de **preço** fica no `commercial`.

---

## Máquina de estados
```
EXPECTED ──pay(paid_amount, paid_at)──▶ PAID
   │   └──(derivado) due_date < hoje ⇒ exibido como OVERDUE
   ▲
   └──unpay──┘   (reverte PAID → EXPECTED; correção de baixa errada)
```

---

## Contratos REST — `InstallmentController`, base `/api/v1/installments`

`targetSector = COMMERCIAL`, `targetOwnerId` resolvido no service (ver RBAC).

### DTO de resposta
```java
InstallmentResponseDTO(
  UUID id, UUID dealId, UUID customerId, String customerName,
  Integer sequence, Integer totalInstallments,
  LocalDate dueDate, BigDecimal expectedAmount,
  InstallmentStatus status, boolean overdue,   // overdue derivado no read
  BigDecimal paidAmount, LocalDateTime paidAt
)
```

| Verbo + path | Corpo | Regra | Status | Action |
|---|---|---|---|---|
| `GET /installments?month=YYYY-MM&status=` | — | a receber/recebido **do mês** | 200 | READ |
| `GET /installments?customerId=` | — | **extrato do cliente** (parcela X de N) | 200 | READ |
| `GET /installments/cashflow?from=&to=` | — | **caixa mês a mês** (recebido/aReceber por mês) | 200 | READ |
| `PATCH /installments/{id}/pay` | `{paidAmount?, paidAt?}` | `EXPECTED → PAID` (paidAmount default = expectedAmount) | 200 · 422 se já PAID | UPDATE |
| `PATCH /installments/{id}/unpay` | — | `PAID → EXPECTED` (corrige baixa) | 200 · 422 se EXPECTED | UPDATE |

> Listagens scope-aware (ADR-013/015). `cashflow` agrega no banco por mês (`due_date`), sem N+1.

---

## RBAC — seed `PermissionRule` (Resource = `INSTALLMENT`, novo no enum)

Confirmação de pagamento é mais sensível que fechar deal → **recepção (`ATTENDANT`) fica de fora**; quem dá baixa é comercial/gestão.

| Role | Actions | Scope |
|---|---|---|
| `USER_COMMERCIAL` | READ, UPDATE | `SECTOR` (COMMERCIAL) |
| `ADM_COMMERCIAL` | READ, UPDATE | `GLOBAL` |
| `ADM_SYSTEM` | READ, UPDATE | `GLOBAL` |

> Default seedado; ajustável se a clínica delegar baixa à recepção.

---

## Impacto no Analytics

`AnalyticsServiceImpl:265` calcula `totalExpectedCash = Σ finalValue × conversionFactor` de deals fechados — projeção cega. Passa a **derivar de `Installment`**:
- `recebido = Σ paid_amount` (status PAID) no período/mês.
- `aReceber = Σ expected_amount` (status EXPECTED) no período/mês.
- `conversionFactor` (caixa líquido após taxa do meio de pagamento) continua conceito do Analytics — aplicado sobre os valores, não no `Installment` (que guarda o **bruto devido**).

`GlobalDashBoardResultDTO` ganha `recebido`/`aReceber` (pequeno breaking interno). Analytics lê `InstallmentRepository` (cross-module, consistente com o padrão de leitura cross-db já adotado).

---

## Consequências

**Positivas**
- Resolve a dor real: a receber por mês, extrato por cliente, caixa futuro — sem busca manual.
- Fronteira limpa: `financeiro` consome (evento + provider); `commercial` é dono do valor; `analytics` deriva de fato real.
- Simetria com `appointment` (mesmo evento, mesmo padrão de filhos-chapados) — baixo custo cognitivo.
- À vista é caso particular (N=1) — sem ramo de código separado.

**Negativas / riscos**
- `commercial` ganha `installmentCount` no `Deal` + `CloseDealRequestDTO` (preço de automatizar o schedule).
- Analytics muda Result DTO (recebido/aReceber).
- 🚫 **Guarda de escopo (NÃO entra agora — vira ERP):** juros/multa por atraso · renegociação de plano · pagamento **parcial** de uma parcela · conciliação bancária/gateway/boleto/PIX automático · NF-e · job noturno de OVERDUE. Cada um é incremento com ADR própria.

**Alternativas consideradas**
- `Deal.paymentStatus` binário (ADR-031): descartado — não resolve parcelamento, a dor real; vira "só um botão".
- Enriquecer o `DealWonEvent` com dados financeiros: descartado — retrabalho/regressão no `appointment` já testado.
- Segundo evento (`DealClosedFinancialEvent`): descartado — 2 eventos p/ o mesmo fato (smell) e **vaza a regra de preço** (`finalValue ?? totalValue`) pro `financeiro`. O provider mantém a regra no `commercial`.
- Plano definido manualmente no `financeiro` pós-WIN: descartado — reintroduz o trabalho manual que a feature elimina.

---

## Próximo passo

1. `Resource.INSTALLMENT` + `InstallmentStatus` (core/enums).
2. `Deal.installmentCount` + `CloseDealRequestDTO.installmentCount`; `closeDeal` persiste.
3. `DealFinancialProvider` + `DealFinancialView` (commercial).
4. `Installment` (@Entity + @TenantId + índices) + repository + Specifications.
5. `InstallmentEventListener` (síncrono) materializa o schedule no WIN.
6. `InstallmentService`/`Impl` (pay/unpay/queries) + `InstallmentController` + DTOs.
7. RBAC seed `Resource.INSTALLMENT`.
8. Analytics: desdobrar `recebido`/`aReceber` a partir de `Installment`.
9. Promover esta ADR → Aceito quando o módulo estiver implementado + testado.
