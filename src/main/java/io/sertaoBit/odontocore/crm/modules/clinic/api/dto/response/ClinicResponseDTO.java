package io.sertaoBit.odontocore.crm.modules.clinic.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CNPJ;

import java.util.List;

public record ClinicResponseDTO(
        @NotBlank String name,
        @CNPJ String cnpj,
        @NotBlank String telephone,
        @NotBlank String address,
        @NotBlank String city,
        List<UserResponseDTO> employees
) {
}
