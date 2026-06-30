package io.sertaoBit.odontocore.crm.modules.financial.repository;

import java.math.BigDecimal;

/**
 * Projeção da query de cashflow (GROUP BY ano+mês de due_date).
 * A JPQL não constrói YearMonth direto; o service monta YearMonth.of(year, month).
 */
public record CashflowRow(
        Integer year,
        Integer month,
        BigDecimal recebido,
        BigDecimal aReceber
) {
}
