package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadTicketUpdateRequestDTO(
        @NotNull UUID assignedTo,
        @NotNull LocalDateTime scheduledAt
) {
}
