package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import io.sertaoBit.odontocore.crm.core.enums.Sector;

import java.math.BigDecimal;

public record SectorDropOffResultDTO(
        Sector sector,
        Long entryCount,
        Long exitCount,
        Long lossCount,
        BigDecimal dropOffPct
) {
}
