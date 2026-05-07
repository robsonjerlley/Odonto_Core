package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadTicketUpdateRequestDTO(
        UUID assignedTo,
        LocalDateTime scheduledAt
) {
}
