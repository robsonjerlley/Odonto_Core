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
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import io.sertaoBit.odontocore.crm.modules.identity.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.*;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.USER;

@Service
public class UserServiceImpl implements UserService {


    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;

    public UserServiceImpl(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            UserMapper userMapper,
            SecurityUtils securityUtils, PermissionService permissionService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.securityUtils = securityUtils;
        this.permissionService = permissionService;
    }


    @Override
    @Transactional
    public UserResponseDTO create(UserCreateRequestDTO dto) {
        var userChek = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                userChek,
                USER,
                CREATE,
                null,
                null
        );

        User newUser = userMapper.toEntity(dto);
        newUser.setActive(true);
        newUser.setCreatedBy(securityUtils.getCurrentUserId());
        newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
        return userMapper.toResponseDTO(userRepository.save(newUser));
    }

    @Override
    @Transactional
    public UserResponseDTO updatePassword(String username, String newPassword) throws IllegalArgumentException {
        var userChek = securityUtils.getCurrentUser();

        permissionService.checkOrThrow(
                userChek,
                USER,
                UPDATE,
                null,
                null
        );
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (!passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        } else {
            throw new IllegalArgumentException("Nova senha não deve ser igual a senha atual.");
        }

        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> search(Sector sector, Role role, Pageable pageable) {
        var user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                USER,
                READ,
                null,
                null
        );

        if (sector != null && role != null) return findAllBySectorAndRole(sector, role, pageable);
        if (sector != null) return findBySector(sector, pageable);
        return findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        var user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                USER,
                READ,
                null,
                null
        );

        return userRepository.findById(id)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findByUsername(String username) {
        var user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                USER,
                READ,
                null,
                null
        );

        return userRepository.findByUsername(username)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!" + username));
    }


    private Page<UserResponseDTO> findAll(Pageable pageable) {

        return userRepository.findAll(pageable)
                .map(userMapper::toResponseDTO);
    }

    private Page<UserResponseDTO> findBySector(Sector sector, Pageable pageable) {
        return userRepository.findBySector(sector, pageable)
                .map(userMapper::toResponseDTO);

    }


    private Page<UserResponseDTO> findAllBySectorAndRole(Sector sector, Role role, Pageable pageable) {
        return userRepository.findAllBySectorAndRole(sector, role, pageable)
                .map(userMapper::toResponseDTO);
    }


    @Override
    @Transactional
    public void delete(UUID id) {
        var user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                USER,
                DELETE,
                null,
                null
        );

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found!" + id);
        }
        userRepository.deleteById(id);
    }

}
