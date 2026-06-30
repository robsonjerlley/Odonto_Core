# ADR-031: `commercial`/financeiro — `Deal.paymentStatus` (fato concreto de liquidação)

**Status**: **Substituída pela ADR-032** (2026-06-30) — a decisão evoluiu de flag `paymentStatus` binário no `Deal` para um **módulo `financeiro` com parcelas (`Installment`)** via `DealWonEvent`. O recorte binário não resolvia a dor real (parcelamento mês a mês); o Analytics já projetava cego, então o flag agregava pouco. Mantida como registro histórico do caminho considerado.
**Data**: 2026-06-27
**Autores**: P.O.-Agent + Arquiteto-Agent + Robson
**Impacto**: `commercial` (`Deal`); `analytics` (`totalExpectedCash`); semente do futuro módulo `financeiro`
**Relaciona**: ADR-029 (appointment **consome** este status no feed, não o define), ADR-030 (Home feed §4 "Pagamentos pendentes"), ADR-026/028 (catálogo/read-model)

> Esta ADR existe porque a decisão foi originalmente discutida dentro da ADR-029 (appointment) — fora de lugar. `paymentStatus` é dado do `Deal`, não do `Appointment`. Movido para cá para manter as fronteiras de módulo limpas.

---

## Contexto

A Home feed (ADR-030 §4) tem uma seção "Pagamentos pendentes". Isso exige saber se um deal fechado foi pago. Hoje **não existe esse fato concreto**: o `AnalyticsServiceImpl` (linha 265-267) já projeta `totalExpectedCash` somando `finalValue × paymentMethod.conversionFactor` de todo deal fechado — mas é uma **projeção cega**, que assume que todo deal pagará.

O financeiro é **parte sequenciada** do OdontoCore (depende da concretização do CRM), não um módulo fora do produto.

---

## Decisão

`Deal.paymentStatus = PENDING | PAID`.
- Default `PENDING` no WIN (fechamento do deal).
- Vira `PAID` por ação explícita ("marcar pago" — micro-ação da Home feed, ADR-030 §4).
- **Mora no `Deal`** (`commercial`): o `Deal` já tem `finalValue`/`paymentMethod`, e a projeção do Analytics agrega **por deal**. Granularidade per-sessão divergiria do que já existe.

### Ganho imediato no Analytics (zero infra nova)
`totalExpectedCash` se desdobra em `recebido` (Σ deals PAID) + `aReceber` (Σ deals PENDING) — só um filtro na query que já roda. O feed "Pagamentos pendentes" (consumido pelo appointment) e o "a receber" (Analytics) passam a ser **o mesmo dado, duas telas**.

---

## Fronteira que se segura (⚠️ MVP = binário)

`paymentStatus` responde **"quitou? sim/não"** — não guarda *quanto* nem *parcelas*.

Limitação assumida: para deal **parcelado** (ex.: R$3.000 em 6×), enquanto não quitar conta 100% em `aReceber`, mesmo com parcelas já recebidas → o "a receber" fica **superestimado**. Aceito no MVP porque (a) o feed de cobrança só precisa de "quitado/não", e (b) já é estritamente melhor que hoje (que conta deal quitado como a receber).

---

## Evolução sequenciada (módulo financeiro — NÃO neste MVP)

1. `amountPaid` (total recebido) → `aReceber = finalValue − amountPaid` preciso.
2. Parcelas com vencimento + conciliação. O `conversionFactor` por `paymentMethod` já é o gancho.

Cada passo é incremento do **financeiro**, com ADR própria quando chegar.

---

## Consequências

**Positivas**
- Fronteira limpa: appointment consome, commercial é dono, analytics deriva.
- Dashboard deixa de contar deal quitado como "a receber" — melhora com custo mínimo.

**Negativas / riscos**
- `aReceber` impreciso para parcelado até o passo 1 da evolução.
- ⚠️ Risco de scope creep: pressão para adicionar valor/parcela "já que estamos aqui". Segurar no binário no MVP.

**Alternativas consideradas**
- `paymentStatus` no `Appointment` (per-sessão): descartado — diverge da granularidade por-deal do Analytics e espalha um conceito de `commercial` pelo `appointment`.
- `amountPaid` já no MVP: descartado — é financeiro de verdade, fora do escopo do que a Home feed precisa agora.

---

## Próximo passo

> ⚠️ **Nota (2026-06-28) — sem migration nesta fase.** O passo 1 abaixo está desatualizado em relação à
> [ADR-027](ADR-027-boot-fixes-schema-flyway-tenant-sentinel.md): o projeto roda greenfield com
> `ddl-auto=update`. A coluna `payment_status` **nasce do campo no `@Entity Deal`** (enum `@Enumerated(STRING)`,
> default `PENDING`), igual às demais — **não se escreve migration de tabela** agora. DDL no Flyway só no
> primeiro deploy, quando o schema migrar para `ddl-auto=validate`.

1. ~~Migration: adicionar `payment_status` em `crm_db.deals`~~ → adicionar o campo `paymentStatus` ao `@Entity Deal` (default `PENDING`); a coluna nasce via `ddl-auto=update`.
2. `closeDeal` seta `PENDING` no WIN; endpoint/ação "marcar pago" → `PAID`.
3. Analytics: desdobrar `totalExpectedCash` em `recebido`/`aReceber`.
4. Promover esta ADR → Aceito.
