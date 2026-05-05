package io.sertaoBit.odontocore.crm.modules.identity.api.dto.response;


import io.sertaoBit.odontocore.crm.core.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserResponseDTO(
        @NotNull UUID id,
        @NotNull @NotBlank String name,
        @NotNull @NotBlank String username,
        @NotNull Role role) {
}
