package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket;

import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record LeadTicketChangeStatusRequestDTO(
        @NotNull TicketStatus status,
        LocalDateTime returnScheduledAt,
        String lossReason
) {
}
