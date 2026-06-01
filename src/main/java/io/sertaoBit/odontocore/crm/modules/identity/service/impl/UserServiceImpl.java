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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    private Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserCreateRequestDTO dto) {
        User newUser = userMapper.toEntity(dto);
        newUser.setActive(true);
        newUser.setCreatedBy(securityUtils.getCurrentUserId());
        newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
        return userMapper.toResponseDTO(userRepository.save(newUser));
    }


    @Override
    @Transactional
    public UserResponseDTO register(UserCreateRequestDTO dto) {
        User newUser = userMapper.toEntity(dto);
        newUser.setActive(true);
        newUser.setRole(Role.USER_ATTENDANT);
        newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
        return userMapper.toResponseDTO(userRepository.save(newUser));
    }


    @Override
    @Transactional
    public UserResponseDTO updatePassword(String username, String newpassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        user.setPasswordHash(passwordEncoder.encode(newpassword));

        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> search(Sector sector, Role role, Pageable pageable) {
        if (sector != null && role != null) return findAllBySectorAndRole(sector, role, pageable);
        if (sector != null) return findBySector(sector, pageable);
        return findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {

        return userRepository.findById(id)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!" + username));
    }


    private Page<UserResponseDTO> findBySector(Sector sector, Pageable pageable) {
        return userRepository.findBySector(sector, pageable)
                .map(userMapper::toResponseDTO);

    }


    private Page<UserResponseDTO> findAllBySectorAndRole(Sector sector, Role role, Pageable pageable) {
        Objects.requireNonNull(sector, "sector must not be null");
        Objects.requireNonNull(role, "role must not be null");

        return userRepository.findAllBySectorAndRole(sector, role, pageable)
                .map(userMapper::toResponseDTO);
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
