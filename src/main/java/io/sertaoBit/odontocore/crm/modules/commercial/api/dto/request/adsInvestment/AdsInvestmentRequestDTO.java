package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdsInvestmentRequestDTO(
        @NotNull AdsChannel channel,
        String campaign,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd

) {
}
