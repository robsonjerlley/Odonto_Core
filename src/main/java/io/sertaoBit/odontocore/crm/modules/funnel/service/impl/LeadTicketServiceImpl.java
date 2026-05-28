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
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.*;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.TICKET;
import static io.sertaoBit.odontocore.crm.core.enums.Role.USER_ATTENDANT;
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
    private final PermissionService permissionService;


    public LeadTicketServiceImpl(
            LeadTicketRepository ticketRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            LeadTicketMapper ticketMapper, SecurityUtils securityUtils, ContactLogRepository contactLogRepository, PermissionService permissionService
    ) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.ticketMapper = ticketMapper;
        this.securityUtils = securityUtils;
        this.contactLogRepository = contactLogRepository;
        this.permissionService = permissionService;
    }


    @Override
    @Transactional
    public LeadTicketResponseDTO create(LeadTicketCreateRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                TICKET,
                CREATE,
                user.getSector(),
                user.getId()
        );

        if (!customerRepository.existsById(dto.customerId())) {
            throw new ResourceNotFoundException("Customer not found: " + dto.customerId());
        }

        LeadTicket leadTicket = LeadTicket.builder()
                .customerId(dto.customerId())
                .status(NEW)
                .currentSector(dto.currentSector())
                .assignedTo(dto.assignedTo())
                .scheduledAt(dto.scheduledAt())
                .createdBy(user.getId())
                .build();

        return ticketMapper.toResponseDTO(ticketRepository.save(leadTicket));
    }


    @Override
    @Transactional
    public LeadTicketResponseDTO changeStatus(UUID id, TicketStatus status) {
        User user = securityUtils.getCurrentUser();

        LeadTicket leadTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found by id: " + id));

        permissionService.checkOrThrow(
                user,
                TICKET,
                UPDATE,
                leadTicket.getCurrentSector(),
                leadTicket.getCreatedBy()
        );

        if (user.getRole() == USER_ATTENDANT
                && (status == LOSS || status == IN_CONTACT)
        ) {
            throw new AccessDeniedException("Perfil de usuário não autorizdado para efetuar esta trazação.");
        }

        var currentStatus = leadTicket.getStatus();
        Set<TicketStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);


        if (allowed == null || !allowed.contains(status)) {
            throw new IllegalStateException("Transition not allowed " + currentStatus + " -> " + status);
        }

        if (status == SCHEDULED) {
            Customer customer = customerRepository.findById(leadTicket.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
            if (customer.getCpf() == null || customer.getCpf().isBlank()) {
                throw new IllegalStateException("CPF é obrigatório para a formalização do agendamento.");
            }
        }


        leadTicket.setStatus(status);

        LocalDateTime now = LocalDateTime.now();
        if (status == WIN) leadTicket.setClosedAt(now);
        if (status == PENDING) leadTicket.setPendingAt(now);
        if (status == RECYCLED) leadTicket.setRecycledAt(now);
        if (status == LOSS) leadTicket.setClosedAt(now);


        ContactLog log = ContactLog.builder()
                .ticketId(id)
                .userId(user.getId())
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
    public Page<LeadTicketResponseDTO> search(
            UUID customerId, TicketStatus status, UUID userId, Pageable pageable
    ) {
        if (customerId != null) return findByCustomer(customerId, pageable);
        if (status != null) return findByStatus(status, pageable);
        if (userId != null) return findByAssignedToUser(userId, pageable);
        return findAll(pageable);
    }


    private Page<LeadTicketResponseDTO> findAll(Pageable pageable) {
        return ticketRepository.findAll(pageable)
                .map(ticketMapper::toResponseDTO);

    }


    private Page<LeadTicketResponseDTO> findByCustomer(UUID customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found by id: " + customerId);
        }

        return ticketRepository.findByCustomerId(customerId, pageable)
                .map(ticketMapper::toResponseDTO);
    }


    private Page<LeadTicketResponseDTO> findByStatus(TicketStatus status, Pageable pageable) {
        return ticketRepository.findByStatus(status, pageable)
                .map(ticketMapper::toResponseDTO);
    }


    private Page<LeadTicketResponseDTO> findByAssignedToUser(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found by id: " + userId);
        }

        return ticketRepository.findByAssignedTo(userId, pageable)
                .map(ticketMapper::toResponseDTO);
    }


    @Override
    @Transactional
    public void deleteById(UUID id) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                TICKET,
                DELETE,
                user.getSector(),
                user.getId()
        );

        if (!ticketRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ticket not found by id: " + id);
        }

        ticketRepository.deleteById(id);

    }
}
