package io.sertaoBit.odontocore.crm.modules.identity.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.mapper.UserMapper;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {


    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    public UserServiceImpl(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            UserMapper userMapper,
            SecurityUtils securityUtils
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserCreateRequestDTO dto) {
        User newUser = userMapper.toEntity(dto);
        newUser.setCreatedBy(securityUtils.getCurrentUserId());
        newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
        return userMapper.toResponseDTO(userRepository.save(newUser));
    }


    @Override
    @Transactional
    public UserResponseDTO register(UserCreateRequestDTO dto) {
        User newUser = userMapper.toEntity(dto);
        newUser.setRole(Role.USER_ATTENDANT);
        newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
        return userMapper.toResponseDTO(userRepository.save(newUser));
    }


    @Override
    @Transactional
    public UserResponseDTO updatePassword(String username, String newpassword) {
        Objects.requireNonNull(username, "username must not be null");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        user.setPasswordHash(passwordEncoder.encode(newpassword));

        return userMapper.toResponseDTO(userRepository.save(user));
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findByUsername(String username) {
        Objects.requireNonNull(username, "username must not be null");

        return userRepository.findByUsername(username)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!" + username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findBySector(Sector sector) {
        Objects.requireNonNull(sector, "sector must not be null");

        return userRepository.findBySector(sector).stream()
                .map(userMapper::toResponseDTO).toList();

    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAllBySectorAndRole(Sector sector, Role role) {
        Objects.requireNonNull(sector, "sector must not be null");
        Objects.requireNonNull(role, "role must not be null");

        return userRepository.findAllBySectorAndRole(sector, role).stream()
                .map(userMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean existsByUsername(String username) {
        Objects.requireNonNull(username, "username must not be null");
        return userRepository.existsByUsername(username);
    }


    @Override
    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found!");
        }
        userRepository.deleteById(id);
    }

}
