package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

public record CustomerUpdateRequestDTO(
        @NotBlank String name,
        @CPF String cpf,
        @NotBlank String telephone,
        @NotBlank String city,
        @NotBlank String address,
        @NotNull TicketStatus ticketStatus
){
}
