package io.sertaoBit.odontocore.crm.modules.funnel.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketChangeStatusRequestDTO;
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
import static io.sertaoBit.odontocore.crm.core.enums.Role.*;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.EVALUATOR;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.LEADS;
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
            RECYCLED, Set.of(NEW),
            WIN, Set.of(POST_PROCEDURE),
            POST_PROCEDURE, Set.of(SCHEDULED, LOSS)
    );


    private static final Map<TicketStatus, Set<Role>> TRANSITION_ROLES = Map.of(
            POST_PROCEDURE, Set.of(USER_ATTENDANT, USER_LEADS, ADM_LEADS, ADM_SYSTEM),
            WIN, Set.of(USER_COMMERCIAL, ADM_COMMERCIAL, ADM_SYSTEM),
            LOSS, Set.of(USER_LEADS, ADM_LEADS, ADM_SYSTEM)
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
    public LeadTicketResponseDTO changeStatus(UUID id, LeadTicketChangeStatusRequestDTO dto) {
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
                && (dto.status() == LOSS || dto.status() == IN_CONTACT)
        ) {
            throw new AccessDeniedException("Perfil de usuário não autorizdado para efetuar esta trazação.");
        }

        var currentStatus = leadTicket.getStatus();
        Set<TicketStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);


        if (allowed == null || !allowed.contains(dto.status())) {
            throw new IllegalStateException("Transition not allowed " + currentStatus + " -> " + dto.status());
        }

        Set<Role> allowedRoles = TRANSITION_ROLES.get(dto.status());
        if (allowedRoles != null) {
            Role currentRole = user.getRole();
            if (!allowedRoles.contains(currentRole)) {
                throw new AccessDeniedException("Role " + currentRole + " não pode executar esta transição");
            }
        }

        if (dto.status() == SCHEDULED) {
            Customer customer = customerRepository.findById(leadTicket.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
            if (customer.getCpf() == null || customer.getCpf().isBlank()) {
                throw new IllegalStateException("CPF é obrigatório para a formalização do agendamento.");
            }
        }


        LocalDateTime now = LocalDateTime.now();

        String logNote;
        if (currentStatus == WIN && dto.status() == POST_PROCEDURE) {
            logNote = applyPostProcedure(leadTicket, now);
        } else if (currentStatus == POST_PROCEDURE && dto.status() == SCHEDULED) {
            logNote = applyScheduledReturn(leadTicket, dto.returnScheduledAt(), now);
        } else if (currentStatus == POST_PROCEDURE && dto.status() == LOSS) {
            logNote = applyLoss(dto.lossReason());
        } else {
            logNote = "Status changed: " + currentStatus + " → " + dto.status();
        }

        leadTicket.setStatus(dto.status());

        if (dto.status() == WIN) leadTicket.setClosedAt(now);
        if (dto.status() == PENDING) leadTicket.setPendingAt(now);
        if (dto.status() == RECYCLED) leadTicket.setRecycledAt(now);
        if (dto.status() == LOSS) leadTicket.setClosedAt(now);


        ContactLog log = ContactLog.builder()
                .ticketId(id)
                .userId(user.getId())
                .channel(ContactChannel.OTHER)
                .note(logNote)
                .statusBefore(currentStatus)
                .statusAfter(dto.status())
                .occurredAt(now)
                .build();
        contactLogRepository.save(log);


        return ticketMapper.toResponseDTO(ticketRepository.save(leadTicket));
    }

    @Override
    @Transactional(readOnly = true)
    public LeadTicketResponseDTO findById(UUID id) {
        User user = securityUtils.getCurrentUser();
        LeadTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found by id: " + id));
        permissionService.checkOrThrow(
                user,
                TICKET,
                READ,
                ticket.getCurrentSector(),
                ticket.getCreatedBy()
        );

        return ticketMapper.toResponseDTO(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadTicketResponseDTO> search(
            UUID customerId, TicketStatus status, UUID userId, Pageable pageable
    ) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                TICKET,
                READ,
                user.getSector(),
                user.getId()
        );

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


    private String applyPostProcedure(LeadTicket ticket, LocalDateTime now) {
        ticket.setProcedurePerformedAt(now);
        ticket.setCurrentSector(LEADS);
        return "Procedimento realizado. Início do acompanhamento pós-procedimento.";
    }

    private String applyScheduledReturn(LeadTicket ticket, LocalDateTime returnScheduledAt, LocalDateTime now) {
        if (returnScheduledAt == null || returnScheduledAt.isBefore(now)) {
            throw new IllegalStateException("returnScheduledAt é obrigatório e deve ser uma data futura.");
        }
        ticket.setCurrentSector(EVALUATOR);
        ticket.setScheduledAt(returnScheduledAt);
        return "Retorno agendado para " + returnScheduledAt + ".";
    }

    private String applyLoss(String lossReason) {
        if (lossReason == null || lossReason.isBlank()) {
            throw new IllegalStateException("lossReason é obrigatório para registrar uma perda.");
        }
        return lossReason;
    }
}
