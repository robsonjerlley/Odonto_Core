package io.sertaoBit.odontocore.crm.config.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthRequestDTO(
        @NotBlank String username,
        @NotBlank String password
) {
}
