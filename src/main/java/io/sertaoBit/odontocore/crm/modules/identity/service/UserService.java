package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.enums.Role;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface UserService {

    UserResponseDTO create(UserCreateRequestDTO requestDTO);
    UserResponseDTO update(String username);
    List<UserResponseDTO> findAll();
    UserResponseDTO findById(UUID id);
    UserResponseDTO findByUsername(String username);

     void delete(UUID id);


}
