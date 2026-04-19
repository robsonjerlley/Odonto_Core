package io.sertaoBit.odontocore.crm.modules.identity.service.impl;

import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.enums.Role;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final IUserRepository userRepository;

    public UserServiceImpl(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserCreateRequestDTO create(String username, String password, Role role) {
        return null;
    }

    @Override
    public UserResponseDTO findByUsername(String username) {
        return null;
    }

    @Override
    public UserCreateRequestDTO update(String username) {
        return null;
    }

    @Override
    public UserResponseDTO delete(String username) {
        return null;
    }
}
