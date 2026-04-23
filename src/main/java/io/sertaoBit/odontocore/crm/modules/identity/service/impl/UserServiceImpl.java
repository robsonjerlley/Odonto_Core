package io.sertaoBit.odontocore.crm.modules.identity.service.impl;

import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.response.ClinicResponseDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.domain.model.Clinic;
import io.sertaoBit.odontocore.crm.modules.clinic.mapper.IClinicMapper;
import io.sertaoBit.odontocore.crm.modules.clinic.repository.IClinicRepository;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.mapper.IUserMapper;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final IUserMapper userMapper;
    private final IClinicRepository clinicRepository;
    private final IClinicMapper clinicMapper;

    public UserServiceImpl(IUserRepository userRepository, IUserMapper userMapper, IClinicRepository clinicRepository, IClinicMapper clinicMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.clinicRepository = clinicRepository;
        this.clinicMapper = clinicMapper;
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserCreateRequestDTO dto) {
        User newUser = userMapper.toEntity(dto);

        if(dto.clinicId() != null){
            Clinic clinic = clinicRepository.findById(dto.clinicId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid clinic ID"));
            newUser.setClinic(clinic);
        }
        return userMapper.toResponseDTO(userRepository.save(newUser));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll()
                .stream().map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO updatePassword(String username, String Newpassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setPassword(Newpassword);

        return userMapper.toResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("User not found!"));

    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("User not found!" + username));
    }

    @Override
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found!");
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicResponseDTO findClinicByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        Clinic clinic = user.getClinic();
        if (clinic == null) {
            throw new RuntimeException("User has no clinic assigned!");
        }
        return clinicMapper.toResponseDTO(clinic);
    }


}
