package io.sertaoBit.odontocore.crm.modules.crm.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponseDTO(
        @NotNull UUID id,
        @NotNull Customer customer,
        @NotNull TicketStatus ticketStatus,
        @NotNull Priority priority,
        @NotNull User assigneToUser,
        @NotBlank String description,
        @NotNull LocalDateTime createdAt,
        @NotNull LocalDateTime updateAt
) {
}
