# ADR-034 (UX/Frontend) — Tela Installment: Gestão de parcelas

**Status:** Proposto (decisões de UX aceitas; pendente implementação frontend)
**Data:** 2026-06-30
**Autoria:** Carla (UI/UX Agent) + Robson
**Relaciona:** ADR-032 (backend `financeiro` — parcelas via 2ª escuta do DealWonEvent), ADR-030 (feed "Pagamentos pendentes" = status, não módulo).
**Natureza:** decisão de UX/produto. Esta é a master; **spec de UX detalhada no espelho frontend** `B:\projects\odontocore.crm.frontend\docs\adr-frontend-004-installment-gestao.md` — manter em sincronia.

---

## 1. Contexto
A ADR-030 deixou o financeiro como "Won't-have" de tela completa (só chip pago/pendente no feed). Com a **ADR-032 já implementada** (parcelas reais + pay/unpay + cashflow), promovemos o financeiro a **tela de gestão dedicada**. O chip no feed permanece; esta tela é o detalhe.

## 2. Decisão
- Tela **Financeiro · Parcelas**, **mês-a-mês** (navegador de mês; `?month=yyyy-MM`).
- **KPI strip:** Recebido / A receber (do `/cashflow`) + Atrasado (soma client-side dos `overdue`).
- **Filtro de status** = Todos / A receber / Pago. **"Atrasado" não é status de enum**; dentro do mês é faceta client-side sobre `overdue`, cross-month vai ao servidor via `overdue=true` (ver [I1]/[I2]).
- **Marcar pago** = sheet "digite uma vez" (`paidAmount` default = esperado, `paidAt` default = hoje) → `PATCH /pay`.
- **Estornar** = destrutivo com confirmação (`PATCH /unpay`, 200 sem corpo → refetch).
- **Histórico por paciente** = drawer (`?customerId`). **Fluxo de caixa** = 1 gráfico simples secundário (única tela do produto que pode ter gráfico).

## 3. Contrato consumido (código real, 2026-06-30)
`InstallmentResponseDTO`: `customerName`, `sequence/totalInstallments`, `dueDate`, `expectedAmount`, `status(EXPECTED|PAID)`, `overdue(bool)`, `paidAmount`, `paidAt`. RBAC `INSTALLMENT` só READ/UPDATE; ADM_SYSTEM/ADM_COMMERCIAL GLOBAL. Detalhe completo no espelho frontend §2.

## 4. Impactos no backend [IMPACTO BACKEND]
- **[I1]** `PaymentStatus` só tem `EXPECTED`/`PAID` — não existe valor de enum `OVERDUE`. Mas "atrasado" **é** predicado de query (`status=EXPECTED AND dueDate < hoje`, ver `InstallmentMapper.isOverdue`), logo **é filtrável no servidor**. Dentro do mês fica client-side por conveniência (as linhas do mês já estão na mão); **cross-month vai ao servidor** via [I2]. O filtro de *status* segue Todos / A receber / Pago.
- **[I2] IMPLEMENTADO (2026-07-01) — endpoint de atrasados cross-month.** `GET /api/v1/installments` com novo param `overdue=true`:
  - retorna todas as parcelas `EXPECTED` com `dueDate < hoje`, **todos os meses**, paginado, sort default `dueDate ASC` (mais velha primeiro);
  - `month` e `overdue` são **mutuamente exclusivos** — enviar os dois → **400**;
  - Specification nova `overdue()` (= `EXPECTED AND dueDate < today`) combinada com `byScope`; `hasStatus`/`dueBetween` não servem;
  - chip/feed global da home usa `Page.totalElements` como contagem — **sem** endpoint de count separado no MVP.
- **[I3] FORA DO MVP.** Sem status parcial: `/pay` quita mesmo com `paidAmount < expectedAmount`. MVP quita e **avisa**; saldo parcial (`PARTIAL` + residual) é impacto backend futuro.

## 5. Próximos passos
1. ~~Implementar `overdue=true` no backend~~ ✅ **feito 2026-07-01** (controller + `overdue()` Specification + validação mútua com `month` → 400; testes service/controller/repo).
2. Implementar `InstallmentRow` + KPI + Sheet pagamento; drawer e cashflow depois.
