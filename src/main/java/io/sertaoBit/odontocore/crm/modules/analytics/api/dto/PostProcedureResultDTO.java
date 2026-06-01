package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import java.math.BigDecimal;

public record PostProcedureResultDTO(
        int totalPostProcedure,
        int returnedCount,
        int lostCount,
        BigDecimal returnRate,
        int pendingCount
) {
}
