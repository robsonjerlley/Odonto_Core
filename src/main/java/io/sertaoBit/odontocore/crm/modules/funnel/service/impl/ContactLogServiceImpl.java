package io.sertaoBit.odontocore.crm.modules.funnel.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Action;
import io.sertaoBit.odontocore.crm.core.enums.Resource;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
        permissionService.checkOrThrow(user, Resource.CONTACT_LOG, Action.CREATE, user.getSector(), null);

        LeadTicket ticket = ticketRepository.findById(dto.ticketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        var userId = user.getId();

        ContactLog contactLog = ContactLog.builder()
                .ticketId(dto.ticketId())
                .userId(userId)
                .channel(dto.channel())
                .note(dto.note())
                .statusBefore(ticket.getStatus())
                .occurredAt(dto.occurredAt())
                .build();

        return contactLogMapper.toResponseDTO(contactLogRepository.save(contactLog));

    }

    @Override
    @Transactional(readOnly = true)
    public ContactLogResponseDTO findById(UUID id) {
        Objects.requireNonNull(id);
        return contactLogRepository.findById(id)
                .map(contactLogMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Contact Log not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findAll() {
        return contactLogRepository.findAll().stream()
                .map(contactLogMapper::toResponseDTO)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findByTicketId(UUID ticketId) {
        return contactLogRepository.findByTicketId(ticketId).stream()
                .map(contactLogMapper::toResponseDTO)
                .toList();
    }


    @Override
    @Transactional
    public void delete(UUID id) {
        if (!contactLogRepository.existsById(id)) {
            throw new ResourceNotFoundException("ContactLog not found by id: " + id);
        }
        contactLogRepository.deleteById(id);
    }
}
