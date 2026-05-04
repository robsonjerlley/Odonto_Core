package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketCreateRequestDTO(
        @NotNull Customer customer,
        @NotNull User assigneToUser,
        @NotNull TicketStatus ticketStatus,
        @NotNull Priority priority,
        @NotBlank String description

) {
}
