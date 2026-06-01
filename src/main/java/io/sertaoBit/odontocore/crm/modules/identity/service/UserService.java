package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;


public interface UserService {

    UserResponseDTO create(UserCreateRequestDTO requestDTO);

    UserResponseDTO register(UserCreateRequestDTO requestDTO);

    UserResponseDTO updatePassword(String username, String newPassword);

    Page<UserResponseDTO> search(Sector sector, Role role, Pageable pageable);

    UserResponseDTO findById(UUID id);

    UserResponseDTO findByUsername(String username);

    void delete(UUID id);

}
