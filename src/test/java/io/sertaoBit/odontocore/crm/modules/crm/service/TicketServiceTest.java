package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket.TicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket.TicketUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.TicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.Priority;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Ticket;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.ITicketMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ITicketRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.impl.TicketServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceTest - Testes Unitários do Serviço de Tickets")
class TicketServiceTest {

    private TicketServiceImpl ticketService;

    @Mock
    private ITicketRepository ticketRepository;

    @Mock
    private ICustomerRepository customerRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private ITicketMapper ticketMapper;

    private UUID ticketId;
    private UUID customerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(
                ticketRepository,
                customerRepository,
                userRepository,
                ticketMapper
        );

        ticketId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Deve criar ticket com sucesso")
    void testCreateTicketSuccess() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);

        User user = new User();
        user.setId(userId);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCustomer(customer);
        ticket.setAssigneTo(user);

        TicketResponseDTO responseDTO = new TicketResponseDTO(
                ticketId, "Ticket Test", TicketStatus.OPEN, Priority.HIGH, null, null, null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ticketMapper.toEntity(any())).thenReturn(ticket);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(responseDTO);

        TicketCreateRequestDTO dto = new TicketCreateRequestDTO(
                new Object() { public UUID getId() { return customerId; } },
                new Object() { public UUID getId() { return userId; } },
                "Ticket Test Description",
                TicketStatus.OPEN,
                Priority.HIGH,
                null
        );

        // Act
        TicketResponseDTO result = ticketService.create(dto);

        // Assert
        assertNotNull(result);
        assertEquals(ticketId, result.id());
        verify(customerRepository, times(1)).findById(customerId);
        verify(userRepository, times(1)).findById(userId);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando customer não encontrado")
    void testCreateTicketCustomerNotFound() {
        // Arrange
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        TicketCreateRequestDTO dto = new TicketCreateRequestDTO(
                new Object() { public UUID getId() { return customerId; } },
                new Object() { public UUID getId() { return userId; } },
                "Ticket Test",
                TicketStatus.OPEN,
                Priority.HIGH,
                null
        );

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.create(dto);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(ticketRepository, never()).save(any());
    }

    // ========== FIND TESTS ==========

    @Test
    @DisplayName("Deve buscar ticket por ID com sucesso")
    void testFindByIdSuccess() {
        // Arrange
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketStatus(TicketStatus.OPEN);

        TicketResponseDTO responseDTO = new TicketResponseDTO(
                ticketId, "Ticket Test", TicketStatus.OPEN, Priority.HIGH, null, null, null
        );

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(responseDTO);

        // Act
        TicketResponseDTO result = ticketService.findById(ticketId);

        // Assert
        assertNotNull(result);
        assertEquals(ticketId, result.id());
        verify(ticketRepository, times(1)).findById(ticketId);
    }

    @Test
    @DisplayName("Deve buscar tickets por status com sucesso")
    void testFindByStatusSuccess() {
        // Arrange
        Ticket ticket1 = new Ticket();
        ticket1.setId(UUID.randomUUID());
        ticket1.setTicketStatus(TicketStatus.OPEN);

        Ticket ticket2 = new Ticket();
        ticket2.setId(UUID.randomUUID());
        ticket2.setTicketStatus(TicketStatus.OPEN);

        List<Ticket> tickets = List.of(ticket1, ticket2);

        TicketResponseDTO dto1 = new TicketResponseDTO(
                ticket1.getId(), "Ticket 1", TicketStatus.OPEN, Priority.HIGH, null, null, null
        );
        TicketResponseDTO dto2 = new TicketResponseDTO(
                ticket2.getId(), "Ticket 2", TicketStatus.OPEN, Priority.MEDIUM, null, null, null
        );

        when(ticketRepository.findAll()).thenReturn(tickets);
        when(ticketMapper.toResponseDTO(ticket1)).thenReturn(dto1);
        when(ticketMapper.toResponseDTO(ticket2)).thenReturn(dto2);

        // Act
        List<TicketResponseDTO> results = ticketService.findByTicketStatus(TicketStatus.OPEN);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(ticketRepository, times(1)).findAll();
    }

    // ========== UPDATE STATUS TESTS ==========

    @Test
    @DisplayName("Deve atualizar status do ticket com sucesso")
    void testUpdateStatusSuccess() {
        // Arrange
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTicketStatus(TicketStatus.OPEN);

        TicketResponseDTO responseDTO = new TicketResponseDTO(
                ticketId, "Ticket Test", TicketStatus.IN_PROGRESS, Priority.HIGH, null, null, null
        );

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(responseDTO);

        // Act
        TicketResponseDTO result = ticketService.updateStatus(ticketId, TicketStatus.IN_PROGRESS);

        // Assert
        assertNotNull(result);
        assertEquals(TicketStatus.IN_PROGRESS, result.ticketStatus());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao atualizar status com null")
    void testUpdateStatusWithNull() {
        // Arrange
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.updateStatus(ticketId, null);
        });

        assertTrue(exception.getMessage().contains("cannot not be null"));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Deve deletar ticket com sucesso")
    void testDeleteTicketSuccess() {
        // Arrange
        when(ticketRepository.existsById(ticketId)).thenReturn(true);

        // Act
        ticketService.deleteById(ticketId);

        // Assert
        verify(ticketRepository, times(1)).deleteById(ticketId);
    }

    @Test
    @DisplayName("Deve lançar erro ao deletar ticket inexistente")
    void testDeleteTicketNotFound() {
        // Arrange
        when(ticketRepository.existsById(ticketId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.deleteById(ticketId);
        });

        assertTrue(exception.getMessage().contains("Ticket not found"));
        verify(ticketRepository, never()).deleteById(any());
    }

    // ========== FIND BY CUSTOMER TESTS ==========

    @Test
    @DisplayName("Deve buscar tickets por customer ID com sucesso")
    void testFindByCustomerSuccess() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCustomer(customer);

        TicketResponseDTO responseDTO = new TicketResponseDTO(
                ticketId, "Ticket Test", TicketStatus.OPEN, Priority.HIGH, null, null, null
        );

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(ticketRepository.findAll()).thenReturn(List.of(ticket));
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(responseDTO);

        // Act
        List<TicketResponseDTO> results = ticketService.findByCustomer(customerId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(customerRepository, times(1)).existsById(customerId);
    }
}

