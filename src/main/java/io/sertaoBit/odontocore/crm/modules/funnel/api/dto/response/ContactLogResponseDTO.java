package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response;

import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContactLogResponseDTO(
        UUID id,
        UUID ticketId,
        UUID userId,
        ContactChannel channel,
        String note,
        TicketStatus statusBefore,
        TicketStatus statusAfter,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {
}
