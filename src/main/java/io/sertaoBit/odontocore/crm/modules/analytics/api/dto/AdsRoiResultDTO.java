package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;

import java.math.BigDecimal;

public record AdsRoiResultDTO(
        AdsChannel channel,
        BigDecimal totalInvestment,
        BigDecimal totalRevenue,
        BigDecimal roiMultiplier,
        Long leadsCount,
        Long closedCount
) {
}
