package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.LeadTicketMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.LeadTicketServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("LeadTicketService - Testes Unitários do Serviço")
public class LeadTicketServiceTest {

    private LeadTicketService leadTicketService;
    @Mock
    private LeadTicketRepository ticketRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ContactLogRepository contactLogRepository;
    @Mock
    private LeadTicketMapper ticketMapper;

    @BeforeEach
    void setUp() {
        leadTicketService = new LeadTicketServiceImpl(
                ticketRepository,
                customerRepository,
                userRepository,
                ticketMapper,
                securityUtils,
                contactLogRepository);
    }

    @Test
    @DisplayName("Deve criar um ticket com sucesso")
    void create() {

        UUID userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(true);

        LeadTicketCreateRequestDTO dto = new LeadTicketCreateRequestDTO(
                customerId, Sector.LEADS, userId,
                LocalDateTime.of(2026, 6, 16, 16, 25)
        );

        LeadTicket leadTicket = LeadTicket.builder()
                .id(UUID.randomUUID())
                .customerId(dto.customerId())
                .status(NEW)
                .currentSector(dto.currentSector())
                .assignedTo(dto.assignedTo())
                .scheduledAt(dto.scheduledAt())
                .createdBy(userId)
                .build();

        when(ticketRepository.save(any(LeadTicket.class))).thenReturn(leadTicket);

        LeadTicketResponseDTO expectedDTO = new LeadTicketResponseDTO(
                leadTicket.getId(),
                leadTicket.getCustomerId(),
                leadTicket.getStatus(),
                leadTicket.getCurrentSector(),
                leadTicket.getAssignedTo(),
                leadTicket.getScheduledAt(),
                null,
                null,
                leadTicket.getCreatedBy(),
                null,
                LocalDateTime.now(),
                null,
                null
        );

        when(ticketMapper.toResponseDTO(any(LeadTicket.class))).thenReturn(expectedDTO);
        LeadTicketResponseDTO result = leadTicketService.create(dto);

        assertNotNull(expectedDTO);
        assertEquals(dto.customerId(), result.customerId());
        assertEquals(dto.currentSector(), result.currentSector());
        assertEquals(dto.scheduledAt(), result.scheduledAt());
        assertEquals(dto.assignedTo(), result.assignedTo());

        verify(ticketRepository, times(1)).save(any(LeadTicket.class));
        verify(securityUtils, times(1)).getCurrentUserId();
        verify(ticketMapper, times(1)).toResponseDTO(any(LeadTicket.class));
    }

    @Test
    @DisplayName("Deve alterar o status do ticket com sucesso")
    void changeStatus() {

        // ARRANGE
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        LeadTicket ticket = LeadTicket.builder()
                .id(ticketId)
                .status(NEGOTIATION)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(ticketRepository.save(any(LeadTicket.class))).thenReturn(ticket);

        LeadTicketResponseDTO expectedResponse = new LeadTicketResponseDTO(
                ticketId,
                UUID.randomUUID(),
                WIN,
                Sector.COMMERCIAL,
                null,
                null,
                null,
                LocalDateTime.now(),
                userId,
                null,
                LocalDateTime.now(),
                null,
                null
        );
        when(ticketMapper.toResponseDTO(any(LeadTicket.class))).thenReturn(expectedResponse);

        // ACT
        LeadTicketResponseDTO result = leadTicketService.changeStatus(ticketId, WIN);

        // ASSERT
        assertNotNull(result);
        assertEquals(WIN, result.status());

        // VERIFY
        verify(ticketRepository).findById(ticketId);
        verify(contactLogRepository).save(any(ContactLog.class));
        verify(ticketRepository).save(any(LeadTicket.class));
        verify(ticketMapper).toResponseDTO(any(LeadTicket.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao criar ticket com customer inexistente")
    void create_customerNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(false);

        LeadTicketCreateRequestDTO dto = new LeadTicketCreateRequestDTO(
                customerId, Sector.LEADS, null, null
        );

        assertThrows(ResourceNotFoundException.class, () -> leadTicketService.create(dto));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao alterar status de ticket inexistente")
    void changeStatus_ticketNotFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> leadTicketService.changeStatus(ticketId, IN_CONTACT));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException em transição inválida de status")
    void changeStatus_invalidTransition() {
        UUID ticketId = UUID.randomUUID();
        LeadTicket ticket = LeadTicket.builder().id(ticketId).status(NEW).build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
                () -> leadTicketService.changeStatus(ticketId, WIN));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve setar closedAt ao mover para WIN e pendingAt ao mover para PENDING")
    void changeStatus_setsAuditDates() {
        UUID ticketId = UUID.randomUUID();

        LeadTicket ticketForWin = LeadTicket.builder().id(ticketId).status(NEGOTIATION).build();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticketForWin));
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponseDTO(any())).thenReturn(null);

        leadTicketService.changeStatus(ticketId, WIN);
        assertNotNull(ticketForWin.getClosedAt());

        LeadTicket ticketForPending = LeadTicket.builder().id(ticketId).status(NEGOTIATION).build();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticketForPending));

        leadTicketService.changeStatus(ticketId, PENDING);
        assertNotNull(ticketForPending.getPendingAt());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar ticket por id inexistente")
    void findById_notFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadTicketService.findById(ticketId));
    }


    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar tickets de customer inexistente")
    void findByCustomer_customerNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> leadTicketService.findByCustomer(customerId));

        verify(ticketRepository, never()).findByCustomerId(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar tickets de usuário inexistente")
    void findByAssignedToUser_userNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> leadTicketService.findByAssignedToUser(userId));

        verify(ticketRepository, never()).findByAssignedTo(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar ticket inexistente")
    void deleteById_notFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.existsById(ticketId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> leadTicketService.deleteById(ticketId));

        verify(ticketRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deve deletar ticket com sucesso")
    void deleteById_success() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.existsById(ticketId)).thenReturn(true);

        leadTicketService.deleteById(ticketId);

        verify(ticketRepository).deleteById(ticketId);
    }
}
