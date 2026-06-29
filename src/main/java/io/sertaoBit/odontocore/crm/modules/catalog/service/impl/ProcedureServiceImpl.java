package io.sertaoBit.odontocore.crm.modules.catalog.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.domain.model.Procedure;
import io.sertaoBit.odontocore.crm.modules.catalog.mapper.ProcedureMapper;
import io.sertaoBit.odontocore.crm.modules.catalog.provider.ProcedureProvider;
import io.sertaoBit.odontocore.crm.modules.catalog.provider.ProcedureView;
import io.sertaoBit.odontocore.crm.modules.catalog.repository.ProcedureRepository;
import io.sertaoBit.odontocore.crm.modules.catalog.service.ProcedureService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.*;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.PROCEDURE;
import static io.sertaoBit.odontocore.crm.modules.catalog.repository.ProcedureSpecifications.*;

@Service
public class ProcedureServiceImpl implements ProcedureService, ProcedureProvider {

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
                .active(true)
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
        procedure.setDefaultPrice(dto.defaultPrice());
        procedure.setCreatedBy(user.getId());

        return procedureMapper.toResponseDTO(procedureRepository.save(procedure));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcedureResponseDTO> search(String name, String code, Pageable pageable) {
        User user = securityUtils.getCurrentUser();

        PermissionScope scope = permissionService.getScope(
                user,
                PROCEDURE,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access denied"));

        Specification<Procedure> spec = Specification
                .where(byScope(scope, user))
                .and(hasName(name))
                .and(hasCode(code))
                .and(isActive());

        return procedureRepository.findAll(spec, pageable)
                .map(procedureMapper::toResponseDTO);
    }


    @Override
    @Transactional
    public void softDelete(UUID procedureId) {
        User user = securityUtils.getCurrentUser();

        permissionService.checkOrThrow(
                user,
                PROCEDURE,
                DELETE,
                null,
                null
        );

        Procedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new ResourceNotFoundException("Procedure not found"));

        procedure.setActive(false);
        procedureRepository.save(procedure);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureView> resolveActiveByIds(List<UUID> ids) {

        List<Procedure> catalog = procedureRepository.findAllById(ids);

        if (catalog.isEmpty()) {
            throw new IllegalArgumentException("Invalid ids provided");
        }

        if (catalog.size() != ids.size()) {
            throw new ResourceNotFoundException("Procedures Not Found. Our does not belong to the clinic");
        }


        return catalog.stream()
                .map(p -> {
                    if (!p.isActive()) {
                        throw new IllegalStateException("Procedures " + p.getId() + " Is Inactive");
                    }
                    return procedureMapper.toView(p);
                }).toList();
    }
}
