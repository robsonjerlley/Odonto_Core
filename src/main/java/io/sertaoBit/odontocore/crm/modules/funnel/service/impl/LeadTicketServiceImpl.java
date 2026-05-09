package io.sertaoBit.odontocore.crm.modules.funnel.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.LeadTicketMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.LeadTicketService;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.*;

@Service
public class LeadTicketServiceImpl implements LeadTicketService {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            NEW, Set.of(IN_CONTACT),
            IN_CONTACT, Set.of(SCHEDULED, LOSS),
            SCHEDULED, Set.of(IN_EVALUATION),
            IN_EVALUATION, Set.of(NEGOTIATION, LOSS),
            NEGOTIATION, Set.of(WIN, PENDING),
            PENDING, Set.of(RECYCLED),
            RECYCLED, Set.of(NEW)
    );


    private final LeadTicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ContactLogRepository contactLogRepository;
    private final LeadTicketMapper ticketMapper;


    public LeadTicketServiceImpl(
            LeadTicketRepository ticketRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            LeadTicketMapper ticketMapper, SecurityUtils securityUtils, ContactLogRepository contactLogRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.ticketMapper = ticketMapper;
        this.securityUtils = securityUtils;
        this.contactLogRepository = contactLogRepository;
    }


    @Override
    @Transactional
    public LeadTicketResponseDTO create(LeadTicketCreateRequestDTO dto) {
             customerRepository.existsById(dto.customerId());

        var userId = securityUtils.getCurrentUserId();
        LeadTicket leadTicket = LeadTicket.builder()
                .customerId(dto.customerId())
                .status(NEW)
                .currentSector(dto.currentSector())
                .assignedTo(dto.assignedTo())
                .scheduledAt(dto.scheduledAt())
                .createdBy(userId)
                .build();

        return ticketMapper.toResponseDTO(ticketRepository.save(leadTicket));
    }


    @Override
    @Transactional
    public LeadTicketResponseDTO changeStatus(UUID id, TicketStatus status) {
        LeadTicket leadTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found by id: " + id));

        var currentStatus = leadTicket.getStatus();
        Set<TicketStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(status)) {
            throw new IllegalStateException("Transition not allowed " + currentStatus + " -> " + status);
        }

        leadTicket.setStatus(status);

        LocalDateTime now = LocalDateTime.now();
        if (status == WIN) leadTicket.setClosedAt(now);
        if (status == PENDING) leadTicket.setPendingAt(now);
        if (status == RECYCLED) leadTicket.setRecycledAt(now);
        if (status == LOSS) leadTicket.setClosedAt(now);


        ContactLog log = ContactLog.builder()
                .ticketId(id)
                .userId(securityUtils.getCurrentUserId())
                .channel(ContactChannel.OTHER)
                .note("Status changed: " + currentStatus + " → " + status)
                .statusBefore(currentStatus)
                .statusAfter(status)
                .occurredAt(now)
                .build();
        contactLogRepository.save(log);


        return ticketMapper.toResponseDTO(ticketRepository.save(leadTicket));
    }

    @Override
    @Transactional(readOnly = true)
    public LeadTicketResponseDTO findById(UUID id) {
        return ticketRepository.findById(id)
                .map(ticketMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found by id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadTicketResponseDTO> findAll() {
        return ticketRepository.findAll().stream()
                .map(ticketMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadTicketResponseDTO> findByCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found by id: " + customerId);
        }

        return ticketRepository.findByCustomerId(customerId).stream()
                .map(ticketMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadTicketResponseDTO> findByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status).stream()
                .map(ticketMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadTicketResponseDTO> findByAssignedToUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found by id: " + userId);
        }

        return ticketRepository.findByAssignedTo(userId).stream()
                .map(ticketMapper::toResponseDTO)
                .toList();
    }


    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (!ticketRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ticket not found by id: " + id);
        }

        ticketRepository.deleteById(id);

    }
}
