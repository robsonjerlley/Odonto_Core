package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.Priority;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TicketCreateRequestDTO(
        @NotNull TicketStatus ticketStatus,
        @NotNull Priority priority,
        @NotNull LocalDate dueDate,
        @NotBlank String description

) {
}
