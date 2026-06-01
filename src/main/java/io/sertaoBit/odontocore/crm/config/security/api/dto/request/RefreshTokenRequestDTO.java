package io.sertaoBit.odontocore.crm.config.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(
      @NotBlank String token
) {
}
