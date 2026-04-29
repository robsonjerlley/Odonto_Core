package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.Priority;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record TicketUpdateRequestDTO(
        @NotNull UUID id,
        @NotNull Customer customer,
        @NotNull TicketStatus ticketStatus,
        @NotNull Priority priority,
        @NotNull LocalDate dueDate,
        @NotNull UUID assigneToId,
        @NotBlank String description
) {
}
