package io.sertaoBit.odontocore.crm.modules.crm.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.identity.domain.Role;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String username,
        Role role) {
}
