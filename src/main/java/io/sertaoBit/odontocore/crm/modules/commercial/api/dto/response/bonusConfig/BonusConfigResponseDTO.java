package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.bonusConfig;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BonusConfigResponseDTO(
        UUID id,
        Sector sector,
        Role role,
        String metricKey,
        BigDecimal bonusPct,
        BigDecimal targetValue,
        String periodRef,
        boolean active,
        LocalDateTime createdAt
) {
}
