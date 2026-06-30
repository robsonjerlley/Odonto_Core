package io.sertaoBit.odontocore.crm.modules.financial.api.dto.response;

import java.math.BigDecimal;
import java.time.YearMonth;

public record CashflowMonthDTO(
        YearMonth month,
        BigDecimal recebido,
        BigDecimal aReceber
) {
}
