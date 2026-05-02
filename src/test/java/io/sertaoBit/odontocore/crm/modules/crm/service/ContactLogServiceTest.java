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

import java.math.BigDecimal;
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


        UUID userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);

        UUID contactLogId = UUID.randomUUID();
        contactLog = new ContactLog();
        contactLog.setId(contactLogId);
        contactLog.setCustomer(customer);
        contactLog.setTicket(ticket);
        contactLog.setContactBy(user);
        contactLog.setContactChannel(ContactChannel.WHATSAPP);
        contactLog.setDescription("Generic Description");
        contactLog.setContactOutcome(ContactOutcome.QUALIFIED_LEAD);
        contactLog.setContactDate(LocalDateTime.now());
        contactLog.setNextFollowUp(null);
        contactLog.setInvestmentAmount(BigDecimal.ONE);
        contactLog.setConversionValue(BigDecimal.ONE);

    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Deve criar contact log com sucesso")
    void testCreateContactLogSuccess() {
        // Arrange
        ContactLogCreateRequestDTO createDTO = ContactLogCreateRequestDTO.builder()
                .customer(customer)
                .ticket(ticket)
                .contactBy(user)
                .contactChannel(contactLog.getContactChannel())
                .description("Create Contact Log")
                .contactOutcome(contactLog.getContactOutcome())
                .nextFollowUp(LocalDate.of(2026,6,15))
                .investmentAmount(BigDecimal.valueOf(5000))
                .conversionValue(BigDecimal.valueOf(15000))
                .build();



        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(contactLogMapper.toEntity(createDTO)).thenReturn(contactLog);
        when(contactLogRepository.save(any(ContactLog.class))).thenReturn(contactLog);




        ContactLogResponseDTO responseDTO = ContactLogResponseDTO.builder()
                .id(contactLog.getId())
                .customer(contactLog.getCustomer())
                .ticket(contactLog.getTicket())
                .contactBy(contactLog.getContactBy())
                .contactChannel(List.of(contactLog.getContactChannel()))
                .description("Generic Description")
                .contactOutcome(contactLog.getContactOutcome())
                .contactDate(contactLog.getContactDate())
                .nextFollowUp(contactLog.getNextFollowUp())
                .investmentAmount(contactLog.getInvestmentAmount())
                .conversionValue(contactLog.getConversionValue())
                .build();


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
        ContactLogCreateRequestDTO createDTO = ContactLogCreateRequestDTO.builder()
                .customer(null)
                .ticket(ticket)
                .contactBy(user)
                .build();

        // Act & Assert


        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                contactLogService.create(createDTO));


        assertTrue(exception.getMessage().contains("Customer must not be null"));
        verify(contactLogRepository, never()).save(any());
    }



    @Test
    @DisplayName("Deve validar que ticket pertence ao customer")
    void testCreateContactLogTicketNotBelongToCustomer() {
        // Arrange
        Customer otherCustomer = new Customer();
        otherCustomer.setId(UUID.randomUUID());
        ticket.setCustomer(otherCustomer);  // Ticket pertence  outro customer

        ContactLogCreateRequestDTO createDTO = ContactLogCreateRequestDTO.builder()
                .customer(customer)
                .ticket(ticket)
                .contactBy(user)
                .contactChannel(contactLog.getContactChannel())
                .description("Generic Description")
                .contactOutcome(contactLog.getContactOutcome())
                .nextFollowUp(LocalDate.now())
                .investmentAmount(BigDecimal.valueOf(5000))
                .conversionValue(BigDecimal.valueOf(15000))
                .build();

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            contactLogService.create(createDTO);
        });

        assertTrue(exception.getMessage().contains("Ticket does not belong to this Customer"));
    }

    // ========== FIND TESTS ==========

    @Test
    @DisplayName("Deve buscar contact log por ID com sucesso")
    void testFindByIdSuccess() {
        // Arrange

        contactLog.setId(UUID.randomUUID());
        ContactLogResponseDTO responseDTO = ContactLogResponseDTO.builder()
                .id(contactLog.getId())
                .customer(contactLog.getCustomer())
                .ticket(ticket)
                .contactBy(contactLog.getContactBy())
                .contactChannel(List.of(contactLog.getContactChannel()))
                .description("Generic Description")
                .contactOutcome(contactLog.getContactOutcome())
                .nextFollowUp(LocalDate.now())
                .investmentAmount(BigDecimal.valueOf(5000))
                .conversionValue(BigDecimal.valueOf(15000))
                .build();

        when(contactLogRepository.findById(contactLog.getId())).thenReturn(Optional.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO result = contactLogService.findById(contactLog.getId());

        // Assert
        assertNotNull(result);
        assertEquals(contactLog.getId(), result.id());
        verify(contactLogRepository, times(1)).findById(contactLog.getId());
    }

    @Test
    @DisplayName("Deve buscar contact logs por customer com sucesso")
    void testFindByCustomerSuccess() {
        // Arrange

        ContactLogResponseDTO responseDTO = ContactLogResponseDTO.builder()
                .id(contactLog.getId())
                .customer(contactLog.getCustomer())
                .ticket(ticket)
                .contactBy(contactLog.getContactBy())
                .contactChannel(List.of(contactLog.getContactChannel()))
                .description("Generic Description")
                .contactOutcome(contactLog.getContactOutcome())
                .nextFollowUp(LocalDate.now())
                .investmentAmount(BigDecimal.valueOf(5000))
                .conversionValue(BigDecimal.valueOf(15000))
                .build();

        when(customerRepository.existsById(customer.getId())).thenReturn(true);
        when(contactLogRepository.findByCustomerId(customer.getId())).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findByCustomer(customer.getId());

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(contactLog.getId(), results.getFirst().id());
        verify(customerRepository, times(1)).existsById(customer.getId());
        verify(contactLogRepository, times(1)).findByCustomerId(customer.getId());
    }




    @Test
    @DisplayName("Deve buscar contact logs por canal com sucesso")
    void testFindByChannelSuccess() {
        // Arrange
        contactLog.setContactChannel(ContactChannel.WHATSAPP);
        ContactLogResponseDTO responseDTO = ContactLogResponseDTO.builder()
                .id(UUID.randomUUID())
                .customer(contactLog.getCustomer())
                .ticket(ticket)
                .contactBy(contactLog.getContactBy())
                .contactChannel(List.of(contactLog.getContactChannel()))
                .description("Generic Description")
                .contactOutcome(contactLog.getContactOutcome())
                .nextFollowUp(LocalDate.now())
                .investmentAmount(BigDecimal.valueOf(5000))
                .conversionValue(BigDecimal.valueOf(15000))
                .build();

        when(contactLogRepository.findByContactChannel(ContactChannel.WHATSAPP)).thenReturn((List.of(contactLog)));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO results = contactLogService.findByChannel(contactLog.getContactChannel()).getFirst();

        // Assert
        assertNotNull(results);
        assertEquals(List.of(ContactChannel.WHATSAPP), results.contactChannel());
    }



    @Test
    @DisplayName("Deve buscar contact logs por outcome com sucesso")
    void testFindByOutcomeSuccess() {
        // Arrange
        contactLog.setContactOutcome(ContactOutcome.SUCCESSFUL);
        ContactLogResponseDTO responseDTO = ContactLogResponseDTO.builder()
                .id(UUID.randomUUID())
                .customer(contactLog.getCustomer())
                .ticket(ticket)
                .contactBy(contactLog.getContactBy())
                .contactChannel(List.of(contactLog.getContactChannel()))
                .description("Generic Description")
                .contactOutcome(contactLog.getContactOutcome())
                .nextFollowUp(LocalDate.now())
                .investmentAmount(BigDecimal.valueOf(5000))
                .conversionValue(BigDecimal.valueOf(15000))
                .build();

        when(contactLogRepository.findByContactOutcomes(ContactOutcome.SUCCESSFUL)).thenReturn(List.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        List<ContactLogResponseDTO> results = contactLogService.findOutcome(ContactOutcome.SUCCESSFUL);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(contactLogRepository, times(1)).findByContactOutcomes(ContactOutcome.SUCCESSFUL);
    }

    // ========== UPDATE TESTS ==========


    @Test
    @DisplayName("Deve atualizar contact log com sucesso")
    void testUpdateContactLogSuccess() {
        // Arrange

        ContactLogUpdateRequestDTO updateRequestDTO = ContactLogUpdateRequestDTO.builder()
                .id(contactLog.getId())
                .customer(customer.getId())
                .ticket(ticket.getId())
                .contactBy(user.getId())
                .contactChannel(ContactChannel.WEBSITE_FROM)
                .description("Update Contact Log")
                .contactOutcome(ContactOutcome.SUCCESSFUL)
                .nextFollowUp(LocalDate.of(2026,6,18))
                .investmentAmount(BigDecimal.valueOf(5800))
                .conversionValue(BigDecimal.valueOf(15000))
                .build();

        when(contactLogRepository.findById(contactLog.getId())).thenReturn(Optional.of(contactLog));
        when(contactLogRepository.save(any(ContactLog.class))).thenReturn(contactLog);

        ContactLogResponseDTO responseDTO = ContactLogResponseDTO.builder()
                .id(UUID.randomUUID())
                .customer(contactLog.getCustomer())
                .ticket(contactLog.getTicket())
                .contactBy(contactLog.getContactBy())
                .contactChannel(List.of(contactLog.getContactChannel()))
                .description(contactLog.getDescription())
                .contactOutcome(contactLog.getContactOutcome())
                .nextFollowUp(contactLog.getNextFollowUp())
                .investmentAmount(contactLog.getInvestmentAmount())
                .conversionValue(contactLog.getConversionValue())
                .build();


        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(responseDTO);

        // Act
        ContactLogResponseDTO result = contactLogService.update(contactLog.getId(), updateRequestDTO);

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
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            contactLogService.delete(contactLog.getId());
        });

        assertTrue(exception.getMessage().contains("ContactLog not found"));
        verify(contactLogRepository, never()).deleteById(any());
    }


}


