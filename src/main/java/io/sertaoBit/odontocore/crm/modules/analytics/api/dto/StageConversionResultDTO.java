package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import java.math.BigDecimal;

public record StageConversionResultDTO(

        Long captureCount,
        Long scheduledCount,
        Long dealCreatedCount,
        Long closedCount,
        BigDecimal leadsConversionPct,
        BigDecimal evaluationConversionPct,
        BigDecimal commercialConversionPct

) {
}
