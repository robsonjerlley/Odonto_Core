package io.sertaoBit.odontocore.crm.modules.clinic.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CNPJ;

import java.util.List;
import java.util.UUID;

public record ClinicResponseDTO(
        @NotNull UUID id,
        @NotBlank String name,
        @CNPJ String cnpj,
        @NotBlank String telephone,
        @NotBlank String address,
        @NotBlank String city,
        List<UserResponseDTO> employees
) {
}
