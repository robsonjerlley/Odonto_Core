package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import io.sertaoBit.odontocore.crm.core.enums.Sector;

import java.math.BigDecimal;

public record StageConversionResultDTO(
        Sector sector,
        Long captureCount,
        Long scheduledCount,
        Long dealCreatedCount,
        Long closedCount,
        BigDecimal leadsConversionPct,
        BigDecimal evaluationConversionPct,
        BigDecimal commercialConversionPct

) {
}
