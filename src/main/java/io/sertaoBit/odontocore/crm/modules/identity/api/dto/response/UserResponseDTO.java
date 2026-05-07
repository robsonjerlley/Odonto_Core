package io.sertaoBit.odontocore.crm.modules.identity.api.dto.response;


import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;

import java.util.UUID;


public record UserResponseDTO(
        UUID id,
        String name,
        String username,
        Sector sector,
        Role role
) {
}
