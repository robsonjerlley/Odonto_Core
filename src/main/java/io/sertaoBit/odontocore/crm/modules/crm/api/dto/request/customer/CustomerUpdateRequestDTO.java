package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.customer;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.util.UUID;

public record CustomerUpdateRequestDTO(
        @NotNull UUID id,
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
