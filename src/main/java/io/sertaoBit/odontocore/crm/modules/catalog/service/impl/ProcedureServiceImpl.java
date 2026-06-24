package io.sertaoBit.odontocore.crm.modules.catalog.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.domain.model.Procedure;
import io.sertaoBit.odontocore.crm.modules.catalog.mapper.ProcedureMapper;
import io.sertaoBit.odontocore.crm.modules.catalog.repository.ProcedureRepository;
import io.sertaoBit.odontocore.crm.modules.catalog.service.ProcedureService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.*;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.PROCEDURE;

@Service
public class ProcedureServiceImpl implements ProcedureService {

    private final ProcedureRepository procedureRepository;
    private final ProcedureMapper procedureMapper;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;

    public ProcedureServiceImpl(
            ProcedureRepository procedureRepository,
            ProcedureMapper procedureMapper,
            SecurityUtils securityUtils,
            PermissionService permissionService
    ) {
        this.procedureRepository = procedureRepository;
        this.procedureMapper = procedureMapper;
        this.securityUtils = securityUtils;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public ProcedureResponseDTO create(ProcedureCreateRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                PROCEDURE,
                CREATE,
                null,
                null
        );

        Procedure procedure = Procedure.builder()
                .clinicId(user.getClinicId())
                .name(dto.name())
                .code(dto.code())
                .active(dto.active())
                .estimatedDuration(dto.estimatedDuration())
                .defaultPrice(dto.defaultPrice())
                .createdBy(user.getId())
                .build();

        return procedureMapper.toResponseDTO(procedureRepository.save(procedure));
    }

    @Override
    @Transactional
    public ProcedureResponseDTO update(UUID procedureId, ProcedureUpdateRequestDTO dto) {
        User user = securityUtils.getCurrentUser();

        var procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new ResourceNotFoundException("Procedure not found"));

        permissionService.checkOrThrow(
                user,
                PROCEDURE,
                UPDATE,
                null,
                null
        );

        procedure.setName(dto.name());
        procedure.setCode(dto.code());
        procedure.setActive(dto.active());
        procedure.setEstimatedDuration(dto.estimatedDuration());
        procedure.setDefaultPrice(dto.defaultPrice());
        procedure.setCreatedBy(user.getId());
        procedure.setUpdatedAt(LocalDateTime.now());

        return procedureMapper.toResponseDTO(procedureRepository.save(procedure));
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedureResponseDTO findByName(String name) {
        User user = securityUtils.getCurrentUser();

        Procedure procedure = procedureRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Procedure not found"));

        permissionService.checkOrThrow(
                user,
                PROCEDURE,
                READ,
                user.getSector(),
                user.getId()
        );

        return procedureMapper.toResponseDTO(procedure);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedureResponseDTO isActive(List<UUID> procedureId) {

        return null;
    }

    @Override
    @Transactional
    public void delete(UUID procedureId) {
        User user = securityUtils.getCurrentUser();

        permissionService.checkOrThrow(
                user,
                PROCEDURE,
                DELETE,
                null,
                null
        );

        Procedure procedure =  procedureRepository.findById(procedureId)
                .orElseThrow(() -> new ResourceNotFoundException("Procedure not found"));

        procedureRepository.delete(procedure);
    }
}
