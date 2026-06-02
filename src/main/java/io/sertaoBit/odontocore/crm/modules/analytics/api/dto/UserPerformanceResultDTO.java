package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import io.sertaoBit.odontocore.crm.core.enums.Sector;

import java.math.BigDecimal;
import java.util.UUID;

public record UserPerformanceResultDTO(
        UUID userId,
        String name,
        Sector sector,
        Long totalAssigned,
        Long totalConverted,
        BigDecimal conversionPct,
        BigDecimal avgTicketValue,
        BigDecimal expectedCash,
        BigDecimal calculatedBonus
) {
}
