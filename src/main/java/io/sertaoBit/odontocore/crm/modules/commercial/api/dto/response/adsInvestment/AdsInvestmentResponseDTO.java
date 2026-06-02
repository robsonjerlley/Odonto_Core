package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.adsInvestment;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdsInvestmentResponseDTO(
        UUID id,
        AdsChannel channel,
        String campaign,
        BigDecimal amount,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDateTime createdAt
) {
}
