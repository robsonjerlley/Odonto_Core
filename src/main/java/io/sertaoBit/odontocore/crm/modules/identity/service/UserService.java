package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.enums.Role;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    UserCreateRequestDTO create(String username, String password, Role role);
    UserResponseDTO findByUsername(String username);
    UserCreateRequestDTO update(String username);
    UserResponseDTO delete(String username);


}
