package io.sertaoBit.odontocore.crm.modules.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record UserPasswordUpdateRequestDTO(
        @NotBlank(message = "A senha não pode estar em branco.") String oldPassword,
        @NotBlank(message = "A nova senha não pode estar em branco.")
        @Size(min = 8, message = "A senha deve conter no mínimo 8 caracteres.") String newPassword) {
}
