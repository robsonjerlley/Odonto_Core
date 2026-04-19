package io.sertaoBit.odontocore.crm.modules.identity.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.identity.domain.enums.Role;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String username,
        Role role) {
}
