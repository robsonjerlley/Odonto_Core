package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.LeadTicketMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.LeadTicketServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("TicketServiceTest - Testes Unitários do Serviço de Tickets")
class TicketServiceTest {

    private LeadTicketServiceImpl ticketService;

    @Mock
    private LeadTicketRepository ticketRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeadTicketMapper ticketMapper;

    private UUID ticketId;
    private UUID customerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ticketService = new LeadTicketServiceImpl(
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

        LeadTicket leadTicket = new LeadTicket();
        leadTicket.setId(ticketId);
        leadTicket.setCustomer(customer);
        leadTicket.setAssigneTo(user);

        LeadTicketResponseDTO responseDTO = mock(LeadTicketResponseDTO.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ticketMapper.toEntity(any())).thenReturn(leadTicket);
        when(ticketRepository.save(any(LeadTicket.class))).thenReturn(leadTicket);
        when(ticketMapper.toResponseDTO(leadTicket)).thenReturn(responseDTO);

        LeadTicketCreateRequestDTO dto = mock(LeadTicketCreateRequestDTO.class);

        // Act
        LeadTicketResponseDTO result = ticketService.create(dto);

        // Assert
        assertNotNull(result);
        assertEquals(ticketId, result.id());
        verify(customerRepository, times(1)).findById(customerId);
        verify(userRepository, times(1)).findById(userId);
        verify(ticketRepository, times(1)).save(any(LeadTicket.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando customerId não encontrado")
    void testCreateTicketCustomerNotFound() {
        // Arrange
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        LeadTicketCreateRequestDTO dto = mock(LeadTicketCreateRequestDTO.class);

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
        LeadTicket leadTicket = new LeadTicket();
        leadTicket.setId(ticketId);
        leadTicket.setTicketStatus(TicketStatus.TICKET_OPEN
        );

        LeadTicketResponseDTO responseDTO = mock(LeadTicketResponseDTO.class);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(leadTicket));
        when(ticketMapper.toResponseDTO(leadTicket)).thenReturn(responseDTO);

        // Act
        LeadTicketResponseDTO result = ticketService.findById(ticketId);

        // Assert
        assertNotNull(result);
        assertEquals(ticketId, result.id());
        verify(ticketRepository, times(1)).findById(ticketId);
    }

    @Test
    @DisplayName("Deve buscar tickets por status com sucesso")
    void testFindByStatusSuccess() {
        // Arrange
        LeadTicket leadTicket1 = new LeadTicket();
        leadTicket1.setId(UUID.randomUUID());
        leadTicket1.setTicketStatus(TicketStatus.TICKET_OPEN);

        LeadTicket leadTicket2 = new LeadTicket();
        leadTicket2.setId(UUID.randomUUID());
        leadTicket2.setTicketStatus(TicketStatus.TICKET_OPEN);

        List<LeadTicket> leadTickets = List.of(leadTicket1, leadTicket2);

        LeadTicketResponseDTO dto1 = mock(LeadTicketResponseDTO.class);

        LeadTicketResponseDTO dto2 = mock(LeadTicketResponseDTO.class);


        when(ticketRepository.findAll()).thenReturn(leadTickets);
        when(ticketMapper.toResponseDTO(leadTicket1)).thenReturn(dto1);
        when(ticketMapper.toResponseDTO(leadTicket2)).thenReturn(dto2);

        // Act
        List<LeadTicketResponseDTO> results = ticketService.findByTicketStatus(TicketStatus.TICKET_OPEN);

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
        LeadTicket leadTicket = new LeadTicket();
        leadTicket.setId(ticketId);
        leadTicket.setTicketStatus(TicketStatus.TICKET_OPEN);

        LeadTicketResponseDTO responseDTO = mock(LeadTicketResponseDTO.class);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(leadTicket));
        when(ticketRepository.save(any(LeadTicket.class))).thenReturn(leadTicket);
        when(ticketMapper.toResponseDTO(leadTicket)).thenReturn(responseDTO);

        // Act
        LeadTicketResponseDTO result = ticketService.updateStatus(ticketId, TicketStatus.TICKET_IN_PROGRESS);

        // Assert
        assertNotNull(result);
        assertEquals(TicketStatus.TICKET_IN_PROGRESS, result.ticketStatus());
        verify(ticketRepository, times(1)).save(any(LeadTicket.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao atualizar status com null")
    void testUpdateStatusWithNull() {
        // Arrange
        LeadTicket leadTicket = new LeadTicket();
        leadTicket.setId(ticketId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(leadTicket));

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
    @DisplayName("Deve buscar tickets por customerId ID com sucesso")
    void testFindByCustomerSuccess() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);

        LeadTicket leadTicket = new LeadTicket();
        leadTicket.setId(ticketId);
        leadTicket.setCustomer(customer);

        LeadTicketResponseDTO responseDTO = mock(LeadTicketResponseDTO.class);

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(ticketRepository.findAll()).thenReturn(List.of(leadTicket));
        when(ticketMapper.toResponseDTO(leadTicket)).thenReturn(responseDTO);

        // Act
        List<LeadTicketResponseDTO> results = ticketService.findByCustomer(customerId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(customerRepository, times(1)).existsById(customerId);
    }
}

