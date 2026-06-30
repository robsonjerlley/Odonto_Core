package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
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
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.LeadTicketServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.READ;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.TICKET;
import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("LeadTicketService - Testes Unitários do Serviço")
public class LeadTicketServiceTest {

    private LeadTicketService leadTicketService;
    @Mock private LeadTicketRepository ticketRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private ContactLogRepository contactLogRepository;
    @Mock private LeadTicketMapper ticketMapper;
    @Mock private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        leadTicketService = new LeadTicketServiceImpl(
                ticketRepository,
                customerRepository,
                ticketMapper,
                securityUtils,
                contactLogRepository,
                permissionService);
    }

    private User buildUser(Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .username("test@test.com")
                .password("hash")
                .sector(Sector.COMMERCIAL)
                .role(role)
                .active(true)
                .build();
    }

    private LeadTicketResponseDTO dummyResponse(UUID ticketId, TicketStatus status) {
        return new LeadTicketResponseDTO(
                ticketId, UUID.randomUUID(), status, Sector.COMMERCIAL,
                null, null, null, null,
                UUID.randomUUID(), null, LocalDateTime.now(), null,
                null, null, null
        );
    }

    // ========== CREATE ==========

    @Test
    @DisplayName("Deve criar um ticket com sucesso")
    void create() {
        User currentUser = buildUser(Role.USER_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);

        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(true);

        LeadTicketCreateRequestDTO dto = new LeadTicketCreateRequestDTO(
                customerId, Sector.LEADS, currentUser.getId(),
                LocalDateTime.of(2026, 6, 16, 16, 25)
        );

        LeadTicket leadTicket = LeadTicket.builder()
                .id(UUID.randomUUID())
                .customerId(dto.customerId())
                .status(NEW)
                .currentSector(dto.currentSector())
                .assignedTo(dto.assignedTo())
                .scheduledAt(dto.scheduledAt())
                .createdBy(currentUser.getId())
                .build();

        when(ticketRepository.save(any(LeadTicket.class))).thenReturn(leadTicket);

        LeadTicketResponseDTO expectedDTO = new LeadTicketResponseDTO(
                leadTicket.getId(),
                leadTicket.getCustomerId(),
                leadTicket.getStatus(),
                leadTicket.getCurrentSector(),
                leadTicket.getAssignedTo(),
                leadTicket.getScheduledAt(),
                null, null,
                leadTicket.getCreatedBy(),
                null, LocalDateTime.now(), null,
                null, null, null
        );

        when(ticketMapper.toResponseDTO(any(LeadTicket.class))).thenReturn(expectedDTO);
        LeadTicketResponseDTO result = leadTicketService.create(dto);

        assertNotNull(expectedDTO);
        assertEquals(dto.customerId(), result.customerId());
        assertEquals(dto.currentSector(), result.currentSector());
        assertEquals(dto.scheduledAt(), result.scheduledAt());
        assertEquals(dto.assignedTo(), result.assignedTo());

        verify(ticketRepository, times(1)).save(any(LeadTicket.class));
        verify(securityUtils, times(1)).getCurrentUser();
        verify(ticketMapper, times(1)).toResponseDTO(any(LeadTicket.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao criar ticket com customer inexistente")
    void create_customerNotFound() {
        User currentUser = buildUser(Role.USER_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);

        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(false);

        LeadTicketCreateRequestDTO dto = new LeadTicketCreateRequestDTO(
                customerId, Sector.LEADS, null, null
        );

        assertThrows(ResourceNotFoundException.class, () -> leadTicketService.create(dto));

        verify(ticketRepository, never()).save(any());
    }

    // ========== CHANGE STATUS ==========

    @Test
    @DisplayName("Deve alterar o status do ticket com sucesso")
    void changeStatus() {
        UUID ticketId = UUID.randomUUID();
        User user = buildUser(Role.USER_COMMERCIAL);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        LeadTicket ticket = LeadTicket.builder()
                .id(ticketId)
                .status(NEGOTIATION)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(LeadTicket.class))).thenReturn(ticket);
        when(ticketMapper.toResponseDTO(any(LeadTicket.class))).thenReturn(dummyResponse(ticketId, WIN));

        LeadTicketResponseDTO result = leadTicketService.changeStatus(
                ticketId, new LeadTicketChangeStatusRequestDTO(WIN, null, null));

        assertNotNull(result);
        assertEquals(WIN, result.status());

        verify(ticketRepository).findById(ticketId);
        verify(contactLogRepository).save(any(ContactLog.class));
        verify(ticketRepository).save(any(LeadTicket.class));
        verify(ticketMapper).toResponseDTO(any(LeadTicket.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao alterar status de ticket inexistente")
    void changeStatus_ticketNotFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> leadTicketService.changeStatus(
                        ticketId, new LeadTicketChangeStatusRequestDTO(IN_CONTACT, null, null)));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException em transição inválida de status")
    void changeStatus_invalidTransition() {
        UUID ticketId = UUID.randomUUID();
        User user = buildUser(Role.ADM_SYSTEM);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        LeadTicket ticket = LeadTicket.builder().id(ticketId).status(NEW).build();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
                () -> leadTicketService.changeStatus(
                        ticketId, new LeadTicketChangeStatusRequestDTO(WIN, null, null)));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve setar closedAt ao mover para WIN e pendingAt ao mover para PAID")
    void changeStatus_setsAuditDates() {
        UUID ticketId = UUID.randomUUID();
        User user = buildUser(Role.USER_COMMERCIAL);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        LeadTicket ticketForWin = LeadTicket.builder().id(ticketId).status(NEGOTIATION).build();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticketForWin));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponseDTO(any())).thenReturn(null);

        leadTicketService.changeStatus(ticketId, new LeadTicketChangeStatusRequestDTO(WIN, null, null));
        assertNotNull(ticketForWin.getClosedAt());

        LeadTicket ticketForPending = LeadTicket.builder().id(ticketId).status(NEGOTIATION).build();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticketForPending));

        leadTicketService.changeStatus(ticketId, new LeadTicketChangeStatusRequestDTO(PENDING, null, null));
        assertNotNull(ticketForPending.getPendingAt());
    }

    // ========== POST_PROCEDURE FLOW (US-PPR) ==========

    @Test
    @DisplayName("Deve mover para POST_PROCEDURE setando procedurePerformedAt e setor LEADS")
    void changeStatus_toPostProcedure_success() {
        UUID ticketId = UUID.randomUUID();
        User user = buildUser(Role.ADM_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        LeadTicket ticket = LeadTicket.builder()
                .id(ticketId)
                .status(WIN)
                .customerId(UUID.randomUUID())
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponseDTO(any())).thenReturn(null);

        leadTicketService.changeStatus(ticketId,
                new LeadTicketChangeStatusRequestDTO(POST_PROCEDURE, null, null));

        assertNotNull(ticket.getProcedurePerformedAt());
        assertEquals(Sector.LEADS, ticket.getCurrentSector());
        assertEquals(POST_PROCEDURE, ticket.getStatus());
        verify(contactLogRepository).save(any(ContactLog.class));
    }

    @Test
    @DisplayName("Deve mover de POST_PROCEDURE para SCHEDULED com returnScheduledAt válido")
    void changeStatus_fromPostProcedure_scheduledReturn_success() {
        UUID ticketId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        User user = buildUser(Role.USER_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        LeadTicket ticket = LeadTicket.builder()
                .id(ticketId)
                .status(POST_PROCEDURE)
                .customerId(customerId)
                .build();

        Customer customer = Customer.builder()
                .id(customerId)
                .cpf("12345678901")
                .build();

        LocalDateTime futureDate = LocalDateTime.now().plusDays(3);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponseDTO(any())).thenReturn(null);

        leadTicketService.changeStatus(ticketId,
                new LeadTicketChangeStatusRequestDTO(SCHEDULED, futureDate, null));

        assertEquals(SCHEDULED, ticket.getStatus());
        assertEquals(Sector.EVALUATOR, ticket.getCurrentSector());
        assertEquals(futureDate, ticket.getScheduledAt());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao agendar retorno sem returnScheduledAt")
    void changeStatus_fromPostProcedure_scheduledReturn_throwsIfNoDate() {
        UUID ticketId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        User user = buildUser(Role.USER_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        LeadTicket ticket = LeadTicket.builder()
                .id(ticketId)
                .status(POST_PROCEDURE)
                .customerId(customerId)
                .build();

        Customer customer = Customer.builder()
                .id(customerId)
                .cpf("12345678901")
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        assertThrows(IllegalStateException.class,
                () -> leadTicketService.changeStatus(ticketId,
                        new LeadTicketChangeStatusRequestDTO(SCHEDULED, null, null)));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve mover de POST_PROCEDURE para LOSS quando lossReason é informado")
    void changeStatus_fromPostProcedure_loss_success() {
        UUID ticketId = UUID.randomUUID();
        User user = buildUser(Role.ADM_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        LeadTicket ticket = LeadTicket.builder()
                .id(ticketId)
                .status(POST_PROCEDURE)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponseDTO(any())).thenReturn(null);

        leadTicketService.changeStatus(ticketId,
                new LeadTicketChangeStatusRequestDTO(LOSS, null, "Paciente desistiu"));

        assertEquals(LOSS, ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao mover para LOSS sem lossReason")
    void changeStatus_fromPostProcedure_loss_throwsIfNoReason() {
        UUID ticketId = UUID.randomUUID();
        User user = buildUser(Role.ADM_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        LeadTicket ticket = LeadTicket.builder()
                .id(ticketId)
                .status(POST_PROCEDURE)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
                () -> leadTicketService.changeStatus(ticketId,
                        new LeadTicketChangeStatusRequestDTO(LOSS, null, null)));

        verify(ticketRepository, never()).save(any());
    }

    // ========== FIND BY ID ==========

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar ticket por id inexistente")
    void findById_notFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadTicketService.findById(ticketId));
    }

    // ========== SEARCH ==========

    @Test
    @DisplayName("Deve retornar página vazia ao buscar tickets de customer inexistente")
    void search_byUnknownCustomer_returnsEmptyPage() {
        UUID customerId = UUID.randomUUID();
        User user = buildUser(Role.ADM_SYSTEM);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, TICKET, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        Page<LeadTicketResponseDTO> result =
                leadTicketService.search(customerId, null, null, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Deve retornar página vazia ao buscar tickets de usuário inexistente")
    void search_byUnknownAssignee_returnsEmptyPage() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(Role.ADM_SYSTEM);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, TICKET, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        Page<LeadTicketResponseDTO> result =
                leadTicketService.search(null, null, userId, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }
}