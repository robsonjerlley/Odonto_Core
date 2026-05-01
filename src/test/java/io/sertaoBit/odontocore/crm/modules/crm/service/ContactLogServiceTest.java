package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
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

    private ContactLog contactLog;
    private Customer customer;
    private Ticket ticket;
    private User user;

    @BeforeEach
    void setUp() {
        contactLogService = new ContactLogServiceImpl(
                contactLogRepository,
                contactLogMapper,
                customerRepository,
                ticketRepository,
                userRepository
        );


        UUID customerId = UUID.randomUUID();
        customer = new Customer();
        customer.setId(customerId);

        UUID ticketId = UUID.randomUUID();
        ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCustomer(customer);

        // Assumindo que o ID do User é Long, como em outros módulos.
        UUID userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);

        UUID contactLogId = UUID.randomUUID();
        contactLog = new ContactLog();
        contactLog.setId(contactLogId);
        contactLog.setCustomer(customer);
        contactLog.setTicket(ticket);
        contactLog.setContactBy(user);
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Deve criar contact log com sucesso")
    void testCreateContactLogSuccess() {
        // Arrange
        ContactLogCreateRequestDTO createDTO = new ContactLogCreateRequestDTO(
                customer.getId(),
                ticket.getId(),
                user.getId(),
                ContactChannel.WHATSAPP,
                "Initial contact",
                ContactOutcome.NO_ANSWER,
                null, null, null
        );
        ContactLogResponseDTO responseDTO = new ContactLogResponseDTO(
                contactLog.getId(), LocalDateTime.now(), "Initial contact",
                ContactChannel.WHATSAPP, ContactOutcome.NO_ANSWER, null,
                null, null, customer.getId(), ticket.getId(), user.getId()
        );

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(contactLogMapper.toEntity(createDTO)).thenReturn(contactLog);
        when(contactLogRepository.save(any(ContactLog.class))).thenReturn(contactLog);
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO result = contactLogService.create(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals(contactLog.getId(), result.id());
        verify(customerRepository, times(1)).findById(customer.getId());
        verify(contactLogRepository, times(1)).save(any(ContactLog.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando customer não encontrado")
    void testCreateContactLogCustomerNotFound() {
        // Arrange
        ContactLogCreateRequestDTO dto = new ContactLogCreateRequestDTO(
                customer.getId(), ticket.getId(), user.getId(), null, null, null, null, null, null
        );

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            contactLogService.create(dto);
        });

        assertTrue(exception.getMessage().contains("Customer not found with ID:"));
        verify(contactLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve validar que ticket pertence ao customer")
    void testCreateContactLogTicketNotBelongToCustomer() {
        // Arrange
        Customer otherCustomer = new Customer();
        otherCustomer.setId(UUID.randomUUID());
        ticket.setCustomer(otherCustomer);  // Ticket pertence a outro customer

        ContactLogCreateRequestDTO dto = new ContactLogCreateRequestDTO(
                customer.getId(), ticket.getId(), user.getId(), null, null, null, null, null, null
        );

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            contactLogService.create(dto);
        });

        assertTrue(exception.getMessage().contains("does not belong to the customer"));
    }

    // ========== FIND TESTS ==========

    @Test
    @DisplayName("Deve buscar contact log por ID com sucesso")
    void testFindByIdSuccess() {
        // Arrange
        ContactLogResponseDTO responseDTO = new ContactLogResponseDTO(contactLog.getId(), null, null, null, null, null, null, null, null, null, null);

        when(contactLogRepository.findById(contactLog.getId())).thenReturn(Optional.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO result = contactLogService.findById(contactLog.getId());

        // Assert
        assertNotNull(result);
        assertEquals(contactLogId, result.id());
        verify(contactLogRepository, times(1)).findById(contactLogId);
    }

    @Test
    @DisplayName("Deve buscar contact logs por customer com sucesso")
    void testFindByCustomerSuccess() {
        // Arrange
        ContactLogResponseDTO responseDTO = new ContactLogResponseDTO(contactLog.getId(), null, null, null, null, null, null, null, null, null, null);

        when(customerRepository.existsById(customer.getId())).thenReturn(true);
        when(contactLogRepository.findByCustomerId(customer.getId())).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findByCustomer(customer.getId());

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(customerRepository, times(1)).existsById(customerId);
    }

    @Test
    @DisplayName("Deve buscar contact logs por canal com sucesso")
    void testFindByChannelSuccess() {
        // Arrange
        contactLog.setContactChannel(ContactChannel.WHATSAPP);
        ContactLogResponseDTO responseDTO = new ContactLogResponseDTO(contactLog.getId(), null, null, ContactChannel.WHATSAPP, null, null, null, null, null, null, null);

        when(contactLogRepository.findByContactChannel(ContactChannel.WHATSAPP)).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findByChannel(ContactChannel.WHATSAPP);

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(ContactChannel.WHATSAPP, results.get(0).contactChannel());
    }

    @Test
    @DisplayName("Deve buscar contact logs por outcome com sucesso")
    void testFindByOutcomeSuccess() {
        // Arrange
        contactLog.setContactOutcome(ContactOutcome.SUCCESSFUL);
        ContactLogResponseDTO responseDTO = new ContactLogResponseDTO(contactLog.getId(), null, null, null, ContactOutcome.SUCCESSFUL, null, null, null, null, null, null);

        when(contactLogRepository.findByContactOutcome(ContactOutcome.SUCCESSFUL)).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findOutcome(ContactOutcome.SUCCESSFUL);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(contactLogRepository, times(1)).findByContactOutcome(ContactOutcome.SUCCESSFUL);
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("Deve atualizar contact log com sucesso")
    void testUpdateContactLogSuccess() {
        // Arrange
        ContactLogUpdateRequestDTO dto = new ContactLogUpdateRequestDTO("New description", ContactChannel.PHONE, ContactOutcome.NO_INTEREST, null, null, null);
        ContactLogResponseDTO responseDTO = new ContactLogResponseDTO(contactLog.getId(), null, "New description", null, null, null, null, null, null, null, null);

        when(contactLogRepository.findById(contactLog.getId())).thenReturn(Optional.of(contactLog));
        doNothing().when(contactLogMapper).updateEntityFromDto(dto, contactLog);
        when(contactLogRepository.save(any(ContactLog.class))).thenReturn(contactLog);
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO result = contactLogService.update(contactLog.getId(), dto);

        // Assert
        assertNotNull(result);
        verify(contactLogRepository, times(1)).save(any(ContactLog.class));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Deve deletar contact log com sucesso")
    void testDeleteSuccess() {
        // Arrange
        when(contactLogRepository.existsById(contactLog.getId())).thenReturn(true);

        // Act
        contactLogService.delete(contactLog.getId());

        // Assert
        verify(contactLogRepository, times(1)).deleteById(contactLog.getId());
    }

    @Test
    @DisplayName("Deve lançar erro ao deletar contact log inexistente")
    void testDeleteNotFound() {
        // Arrange
        when(contactLogRepository.existsById(contactLog.getId())).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            contactLogService.delete(contactLog.getId());
        });

        assertTrue(exception.getMessage().contains("ContactLog not found"));
        verify(contactLogRepository, never()).deleteById(any());
    }

    // ========== PENDING FOLLOW-UP TESTS ==========

    @Test
    @DisplayName("Deve buscar contact logs com follow-up pendente")
    void testFindWithPendingFollowUpSuccess() {
        // Arrange
        contactLog.setNextFollowUp(LocalDate.from(LocalDateTime.now().minusDays(1)));  // No passado = vencido
        ContactLogResponseDTO responseDTO = new ContactLogResponseDTO(contactLog.getId(), null, null, null, null, null, null, null, null, null, null);

        when(contactLogRepository.findByNextFollowUpBeforeAndContactOutcomeNot(any(), any())).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findWithPendingFollowUp();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
    }
}
