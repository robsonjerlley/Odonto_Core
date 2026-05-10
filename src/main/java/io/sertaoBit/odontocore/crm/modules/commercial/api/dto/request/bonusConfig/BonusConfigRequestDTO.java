package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BonusConfigRequestDTO(
        @NotNull Sector sector,
        @NotNull Role role,
        @NotBlank String metricKey,
        @NotNull @Positive BigDecimal bonusPct,
        BigDecimal targetValue,
        @NotBlank String periodRef

) {
}
