package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.util.UUID;

public record CustomerCreateRequestDTO(
        @NotBlank String name,
        @CPF String cpf,
        @NotBlank String telephone,
        @NotBlank String city,
        @NotBlank String address,
        @NotBlank String description,
        @NotNull TicketStatus ticketStatus,
        @NotNull UUID departmentId
) {
}
