package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response;


import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadTicketResponseDTO(
        UUID id,
        UUID customerId,
        TicketStatus status,
        Sector currentSector,
        UUID assignedTo,
        LocalDateTime scheduledAt,
        LocalDateTime pendingAt,
        LocalDateTime closedAt,
        UUID createdBy,
        UUID previousTicketId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime recycledAt
) {
}
