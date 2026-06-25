package io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProcedureResponseDTO(
        UUID id,
        UUID clinicId,
        String name,
        String code,
        boolean active,
        Integer estimatedDuration,
        BigDecimal defaultPrice,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
