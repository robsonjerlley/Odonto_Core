package io.sertaoBit.odontocore.crm.modules.identity.api.dto.request;


import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequestDTO(
        @NotBlank @NotNull String name,
        @NotBlank(message = "Campo nome não deve estar em branco ") String username,
        @NotBlank @Size(min = 8, message = "A senha deve conter o mínimo de 8 caracteres.") String passwordHash,
        @NotNull Sector sector,
        @NotNull Role role
) {
}
