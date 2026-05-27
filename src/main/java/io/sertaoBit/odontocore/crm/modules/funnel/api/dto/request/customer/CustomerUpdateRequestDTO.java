package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.util.UUID;

public record CustomerUpdateRequestDTO(
        @NotNull UUID id,
        @NotBlank String name,
        @CPF String cpf,
        @NotBlank String phone,
        String phone2,
        String email
) {
}
