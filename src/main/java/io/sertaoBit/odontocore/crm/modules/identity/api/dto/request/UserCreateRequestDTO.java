package io.sertaoBit.odontocore.crm.modules.identity.api.dto.request;


import io.sertaoBit.odontocore.crm.shared.enums.Department;
import io.sertaoBit.odontocore.crm.shared.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserCreateRequestDTO(
        @NotBlank @NotNull String name,
        @NotBlank(message = "A senha não pode estar em branco.") String username,
        @NotBlank @Size(min = 8, message = "A senha deve conter no mínimo 8 caracteres.") String password,
        @NotNull Department department,
        @NotNull Role role
) {
}
