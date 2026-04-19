package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request;

import io.sertaoBit.odontocore.crm.modules.identity.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequestDTO(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull Role role) {
}
