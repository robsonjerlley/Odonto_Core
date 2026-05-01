package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactOutcome;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Ticket;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.IContactLogMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.IContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ITicketRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.impl.ContactLogServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactLogServiceTest - Testes Unitários do Serviço de Logs de Contato")
class ContactLogServiceTest {

    private ContactLogServiceImpl contactLogService;

    @Mock
    private IContactLogRepository contactLogRepository;

    @Mock
    private ICustomerRepository customerRepository;

    @Mock
    private ITicketRepository ticketRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IContactLogMapper contactLogMapper;

    private UUID contactLogId;
    private UUID customerId;
    private UUID ticketId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        contactLogService = new ContactLogServiceImpl(
                contactLogRepository,
                contactLogMapper,
                customerRepository,
                ticketRepository,
                userRepository
        );

        contactLogId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Deve criar contact log com sucesso")
    void testCreateContactLogSuccess() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCustomer(customer);

        User user = new User();
        user.setId(userId);

        ContactLog contactLog = new ContactLog();
        contactLog.setId(contactLogId);
        contactLog.setCustomer(customer);
        contactLog.setTicket(ticket);
        contactLog.setContactBy(user);

        ContactLogResponseDTO responseDTO = mock(ContactLogResponseDTO.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(contactLogMapper.toEntity(any())).thenReturn(contactLog);
        when(contactLogRepository.save(any(ContactLog.class))).thenReturn(contactLog);
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        ContactLogCreateRequestDTO dto = mock(ContactLogCreateRequestDTO.class);

        // Act
        ContactLogResponseDTO result = contactLogService.create(dto);

        // Assert
        assertNotNull(result);
        assertEquals(contactLogId, result.id());
        verify(customerRepository, times(1)).findById(customerId);
        verify(contactLogRepository, times(1)).save(any(ContactLog.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando customer não encontrado")
    void testCreateContactLogCustomerNotFound() {
        // Arrange
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        ContactLogCreateRequestDTO dto = mock(ContactLogCreateRequestDTO.class);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            contactLogService.create(dto);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(contactLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve validar que ticket pertence ao customer")
    void testCreateContactLogTicketNotBelongToCustomer() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);

        Customer otherCustomer = new Customer();
        otherCustomer.setId(UUID.randomUUID());

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCustomer(otherCustomer);  // Ticket pertence  outro customer

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ContactLogCreateRequestDTO dto = mock(ContactLogCreateRequestDTO.class);


        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            contactLogService.create(dto);
        });

        assertTrue(exception.getMessage().contains("does not belong to this Customer"));
    }

    // ========== FIND TESTS ==========

    @Test
    @DisplayName("Deve buscar contact log por ID com sucesso")
    void testFindByIdSuccess() {
        // Arrange
        ContactLog contactLog = new ContactLog();
        contactLog.setId(contactLogId);

        ContactLogResponseDTO responseDTO = mock(ContactLogResponseDTO.class);

        when(contactLogRepository.findById(contactLogId)).thenReturn(Optional.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO result = contactLogService.findById(contactLogId);

        // Assert
        assertNotNull(result);
        assertEquals(contactLogId, result.id());
        verify(contactLogRepository, times(1)).findById(contactLogId);
    }

    @Test
    @DisplayName("Deve buscar contact logs por customer com sucesso")
    void testFindByCustomerSuccess() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);

        ContactLog contactLog = new ContactLog();
        contactLog.setId(contactLogId);
        contactLog.setCustomer(customer);

        ContactLogResponseDTO responseDTO = mock(ContactLogResponseDTO.class);

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(contactLogRepository.findAll()).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findByCustomer(customerId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(customerRepository, times(1)).existsById(customerId);
    }

    @Test
    @DisplayName("Deve buscar contact logs por canal com sucesso")
    void testFindByChannelSuccess() {
        // Arrange
        ContactLog contactLog = new ContactLog();
        contactLog.setId(contactLogId);
        contactLog.setContactChannel(ContactChannel.WHATSAPP);

        ContactLogResponseDTO responseDTO = mock(ContactLogResponseDTO.class);

        when(contactLogRepository.findAll()).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO result = contactLogService.findByChannel(ContactChannel.WHATSAPP);

        // Assert
        assertNotNull(result);
        assertEquals(ContactChannel.WHATSAPP, result.contactChannel());
        verify(contactLogRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar contact logs por outcome com sucesso")
    void testFindByOutcomeSuccess() {
        // Arrange
        ContactLog contactLog = new ContactLog();
        contactLog.setId(contactLogId);
        contactLog.setContactOutcome(ContactOutcome.SUCCESSFUL);

        ContactLogResponseDTO responseDTO = mock(ContactLogResponseDTO.class);

        when(contactLogRepository.findAll()).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findOutcome(ContactOutcome.SUCCESSFUL);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(contactLogRepository, times(1)).findAll();
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("Deve atualizar contact log com sucesso")
    void testUpdateContactLogSuccess() {
        // Arrange
        ContactLog contactLog = new ContactLog();
        contactLog.setId(contactLogId);
        contactLog.setDescription("Old description");

        ContactLogResponseDTO responseDTO = mock(ContactLogResponseDTO.class);

        ContactLogUpdateRequestDTO dto = mock(ContactLogUpdateRequestDTO.class);


        when(contactLogRepository.findById(contactLogId)).thenReturn(Optional.of(contactLog));
        when(contactLogRepository.save(any(ContactLog.class))).thenReturn(contactLog);
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO result = contactLogService.update(contactLogId, dto);

        // Assert
        assertNotNull(result);
        verify(contactLogRepository, times(1)).save(any(ContactLog.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Deve deletar contact log com sucesso")
    void testDeleteSuccess() {
        // Arrange
        when(contactLogRepository.existsById(contactLogId)).thenReturn(true);

        // Act
        contactLogService.delete(contactLogId);

        // Assert
        verify(contactLogRepository, times(1)).deleteById(contactLogId);
    }

    @Test
    @DisplayName("Deve lançar erro ao deletar contact log inexistente")
    void testDeleteNotFound() {
        // Arrange
        when(contactLogRepository.existsById(contactLogId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            contactLogService.delete(contactLogId);
        });

        assertTrue(exception.getMessage().contains("ContactLog not found"));
        verify(contactLogRepository, never()).deleteById(any());
    }

    // ========== PENDING FOLLOW-UP TESTS ==========

    @Test
    @DisplayName("Deve buscar contact logs com follow-up pendente")
    void testFindWithPendingFollowUpSuccess() {
        // Arrange
        ContactLog contactLog = new ContactLog();
        contactLog.setId(contactLogId);
        contactLog.setNextFollowUp(LocalDate.from(LocalDateTime.now().minusDays(1)));  // No passado = vencido

        ContactLogResponseDTO responseDTO = mock(ContactLogResponseDTO.class);

        when(contactLogRepository.findAll()).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findWithPendingFollowUp();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }
}

