package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IUserService {

    UserResponseDTO create(UserCreateRequestDTO requestDTO, User user);

    UserResponseDTO updatePassword(String username, String newPassword);

    List<UserResponseDTO> findAll();

    UserResponseDTO findById(UUID id);

    UserResponseDTO findByUsername(String username);

    void delete(UUID id);


}
