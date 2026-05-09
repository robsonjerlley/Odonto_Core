package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket;

import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record LeadTicketChangeStatusRequestDTO(
        @NotNull TicketStatus status
) {
}
