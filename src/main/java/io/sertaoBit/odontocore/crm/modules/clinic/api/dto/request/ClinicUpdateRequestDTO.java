package io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CNPJ;

public record ClinicUpdateRequestDTO(
        @NotBlank String name,
        @CNPJ String cnpj,
        @NotBlank String telephone,
        @NotBlank String address,
        @NotBlank String city
) {
}
