package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.recycleConfig;

import java.time.LocalDateTime;
import java.util.UUID;

public record RecycleConfigResponseDTO(
        UUID id,
        int afterDays,
        boolean active,
        LocalDateTime createdAt
) {
}
