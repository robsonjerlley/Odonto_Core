package io.sertaoBit.odontocore.crm.modules.funnel.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Action;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.ContactLogMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.ContactLogService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.READ;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.CONTACT_LOG;

@Service
public class ContactLogServiceImpl implements ContactLogService {

    private final ContactLogRepository contactLogRepository;
    private final ContactLogMapper contactLogMapper;
    private final LeadTicketRepository ticketRepository;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;

    public ContactLogServiceImpl(
            ContactLogRepository contactLogRepository,
            ContactLogMapper contactLogMapper,
            LeadTicketRepository ticketRepository,
            SecurityUtils securityUtils,
            PermissionService permissionService
    ) {
        this.contactLogRepository = contactLogRepository;
        this.contactLogMapper = contactLogMapper;
        this.ticketRepository = ticketRepository;
        this.securityUtils = securityUtils;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public ContactLogResponseDTO create(ContactLogCreateRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                CONTACT_LOG,
                Action.CREATE,
                user.getSector(),
                user.getId()
        );

        LeadTicket ticket = ticketRepository.findById(dto.ticketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        var userId = user.getId();

        ContactLog contactLog = ContactLog.builder()
                .ticketId(dto.ticketId())
                .userId(userId)
                .channel(dto.channel())
                .note(dto.note())
                .statusBefore(null)
                .statusAfter(null)
                .occurredAt(dto.occurredAt())
                .build();

        return contactLogMapper.toResponseDTO(contactLogRepository.save(contactLog));

    }

    @Override
    @Transactional(readOnly = true)
    public ContactLogResponseDTO findById(UUID id) {
        User user = securityUtils.getCurrentUser();
        ContactLog contactLog = contactLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact Log not found by id: " + id));
        permissionService.checkOrThrow(
                user,
                CONTACT_LOG,
                READ,
                user.getSector(),
                contactLog.getUserId()
        );

        return contactLogMapper.toResponseDTO(contactLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactLogResponseDTO> search(UUID ticketId, Pageable pageable) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                CONTACT_LOG,
                READ,
                user.getSector(),
                user.getId()
        );

        if (ticketId != null) return findByTicketId(ticketId, pageable);
        return findAll(pageable);
    }


    private Page<ContactLogResponseDTO> findAll(Pageable pageable) {
        return contactLogRepository.findAll(pageable)
                .map(contactLogMapper::toResponseDTO);
    }


    private Page<ContactLogResponseDTO> findByTicketId(UUID ticketId, Pageable pageable) {
        return contactLogRepository.findByTicketId(ticketId, pageable)
                .map(contactLogMapper::toResponseDTO);
    }

}
