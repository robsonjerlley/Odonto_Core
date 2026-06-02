package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.deal;

import io.sertaoBit.odontocore.crm.core.enums.Sector;

import java.time.LocalDateTime;
import java.util.UUID;

public record DealHistoryResponseDTO(
        UUID dealId,
        UUID changedBy,
        Sector changedBySector,
        String fieldChanged,
        String valueBefore,
        String valueAfter,
        LocalDateTime occurredAt

) {
}
