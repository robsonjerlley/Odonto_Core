package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface UserService {

    UserResponseDTO create(UserCreateRequestDTO requestDTO, User user);

    UserResponseDTO updatePassword(String username, String newPassword);

    UserResponseDTO findByUsername(String username);

    List<UserResponseDTO> findBySector(Sector sector);

    List<UserResponseDTO> findAllBySectorAndRole(Sector sector, Role role);

    Boolean existsByUsername(String username);

    void delete(UUID id);

}
