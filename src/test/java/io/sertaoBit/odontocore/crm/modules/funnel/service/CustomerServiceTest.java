package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.CustomerMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.CustomerServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.AdsChannel.INSTAGRAM;
import static io.sertaoBit.odontocore.crm.core.enums.CustomerSource.ADS_PAID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceTest - Testes Unitários do Serviço")
class CustomerServiceTest {

    private CustomerServiceImpl customerService;

    @Mock private CustomerRepository customerRepository;
    @Mock private LeadTicketRepository leadTicketRepository;
    @Mock private ContactLogRepository contactLogRepository;
    @Mock private CustomerMapper customerMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(
                customerRepository,
                leadTicketRepository,
                contactLogRepository,
                customerMapper,
                securityUtils,
                permissionService
        );
    }

    private User buildUser(Sector sector) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Atendente Teste")
                .username("atendente@teste.com")
                .passwordHash("hash")
                .sector(sector)
                .role(Role.USER_LEADS)
                .active(true)
                .build();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Deve criar customer e abrir LeadTicket automaticamente")
    void create_deveAbrirLeadTicketComSetoDoUsuarioLogado() {
        // Arrange
        User currentUser = buildUser(Sector.LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);

        CustomerCreateRequestDTO dto = new CustomerCreateRequestDTO(
                "Jão da Silva", "123456789", "83999999",
                "mail", ADS_PAID, INSTAGRAM, "Um novo sorriso",null, null
        );

        Customer savedCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .name(dto.name())
                .cpf(dto.cpf())
                .phone(dto.phone())
                .email(dto.email())
                .source(dto.source())
                .adChannel(dto.adChannel())
                .adCampaign(dto.adCampaign())
                .createdBy(currentUser.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerMapper.toResponseDTO(savedCustomer)).thenReturn(
                new CustomerResponseDTO(
                        savedCustomer.getId(), savedCustomer.getName(), savedCustomer.getCpf(),
                        savedCustomer.getPhone(), savedCustomer.getEmail(), savedCustomer.getSource(),
                        savedCustomer.getAdChannel(), savedCustomer.getAdCampaign(),
                        savedCustomer.getCreatedAt(), savedCustomer.getUpdatedAt(),
                        savedCustomer.getCreatedBy(), null
                )
        );

        // Act
        CustomerResponseDTO result = customerService.create(dto);

        // Assert — customer
        assertNotNull(result);
        assertEquals(dto.name(), result.name());
        assertEquals(currentUser.getId(), result.createdBy());

        // Assert — ticket aberto automaticamente
        ArgumentCaptor<LeadTicket> ticketCaptor = ArgumentCaptor.forClass(LeadTicket.class);
        verify(leadTicketRepository, times(1)).save(ticketCaptor.capture());

        LeadTicket ticket = ticketCaptor.getValue();
        assertEquals(savedCustomer.getId(), ticket.getCustomerId());
        assertEquals(TicketStatus.NEW, ticket.getStatus());
        assertEquals(Sector.LEADS, ticket.getCurrentSector());
        assertEquals(currentUser.getId(), ticket.getCreatedBy());

        // Assert — sequência correta: customer antes, ticket depois
        var orderVerifier = inOrder(customerRepository, leadTicketRepository);
        orderVerifier.verify(customerRepository).save(any(Customer.class));
        orderVerifier.verify(leadTicketRepository).save(any(LeadTicket.class));
    }

    @Test
    @DisplayName("Deve usar o setor do usuário logado no ticket — ATTENDANT abre em ATTENDANT")
    void create_setorDoTicketRefleteSetorDoUsuario() {
        // Arrange
        User attendant = buildUser(Sector.ATTENDANT);
        when(securityUtils.getCurrentUser()).thenReturn(attendant);

        CustomerCreateRequestDTO dto = new CustomerCreateRequestDTO(
                "Maria Souza", "987654321", "83988888",
                null, ADS_PAID, null, null, null
        );

        Customer savedCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .name(dto.name())
                .createdBy(attendant.getId())
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerMapper.toResponseDTO(savedCustomer)).thenReturn(
                new CustomerResponseDTO(savedCustomer.getId(), savedCustomer.getName(),
                        null, null, null, null, null, null, null, null,
                        savedCustomer.getCreatedBy(), null)
        );

        // Act
        customerService.create(dto);

        // Assert
        ArgumentCaptor<LeadTicket> captor = ArgumentCaptor.forClass(LeadTicket.class);
        verify(leadTicketRepository).save(captor.capture());
        assertEquals(Sector.ATTENDANT, captor.getValue().getCurrentSector());
    }

    @Test
    @DisplayName("Deve criar customer com sucesso e retornar DTO correto")
    void create_retornaResponseDTOCorreto() {
        // Arrange
        User currentUser = buildUser(Sector.LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);

        CustomerCreateRequestDTO dto = new CustomerCreateRequestDTO(
                "Jão da Silva", "123456789", "83999999",
                "mail", ADS_PAID, INSTAGRAM, "Um novo sorriso", null
        );

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .name("Jão da Silva")
                .cpf(dto.cpf())
                .phone(dto.phone())
                .email(dto.email())
                .source(dto.source())
                .adChannel(dto.adChannel())
                .adCampaign(dto.adCampaign())
                .createdBy(currentUser.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponseDTO expectedDTO = new CustomerResponseDTO(
                customer.getId(), customer.getName(), customer.getCpf(),
                customer.getPhone(), customer.getEmail(), customer.getSource(),
                customer.getAdChannel(), customer.getAdCampaign(),
                customer.getCreatedAt(), customer.getUpdatedAt(),
                customer.getCreatedBy(), null
        );
        when(customerMapper.toResponseDTO(customer)).thenReturn(expectedDTO);

        // Act
        CustomerResponseDTO result = customerService.create(dto);

        // Assert
        assertNotNull(result);
        assertEquals(dto.name(), result.name());
        assertEquals(dto.cpf(), result.cpf());
        assertEquals(currentUser.getId(), result.createdBy());

        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(securityUtils, times(1)).getCurrentUser();
        verify(customerMapper, times(1)).toResponseDTO(customer);
    }

    // ========== FIND TESTS ==========

    @Test
    @DisplayName("Deve lançar erro quando customerId não encontrado por ID")
    void testFindByIdNotFound() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                customerService.findById(customerId));

        assertTrue(exception.getMessage().contains("Customer not found"));
    }

    @Test
    @DisplayName("Deve buscar customerId por CPF com sucesso")
    void testFindByCpfSuccess() {
        UUID customerId = UUID.randomUUID();
        String cpf = "123.456.789-00";

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCpf(cpf);

        CustomerResponseDTO responseDTO = new CustomerResponseDTO(
                customerId, null, cpf, null, null, null, null,
                null, null, null, null, null
        );

        when(customerRepository.findByCpf(cpf)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponseDTO(customer)).thenReturn(responseDTO);

        CustomerResponseDTO result = customerService.findByCpf(cpf);

        assertNotNull(result);
        assertEquals(cpf, result.cpf());
        verify(customerRepository, times(1)).findByCpf(cpf);
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("Deve atualizar customerId com sucesso")
    void testUpdateCustomerSuccess() {
        String cpf = "123.456.789-00";
        UUID customerId = UUID.randomUUID();

        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setName("João Siva");
        existingCustomer.setCpf(cpf);
        existingCustomer.setPhone("8399875878");

        CustomerUpdateRequestDTO dto = new CustomerUpdateRequestDTO(
                customerId, "João da Silva", cpf, "8399875878", null
        );

        CustomerResponseDTO responseDTO = new CustomerResponseDTO(
                dto.id(), dto.name(), dto.cpf(), dto.phone(),
                null, null, null, null, null, null, null, null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(existingCustomer);
        when(customerMapper.toResponseDTO(existingCustomer)).thenReturn(responseDTO);

        CustomerResponseDTO result = customerService.update(customerId, dto);

        assertNotNull(result);
        assertEquals("João da Silva", result.name());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Deve impedir atualizar customerId com CPF duplicado")
    void testUpdateCustomerDuplicateCPF() {
        UUID customerId = UUID.randomUUID();
        String oldCpf = "123.456.789-00";
        String newCpf = "987.654.321-00";

        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setCpf(oldCpf);
        existingCustomer.setName("Jõao da Silva");
        existingCustomer.setPhone("8399875878");

        Customer otherCustomer = new Customer();
        otherCustomer.setId(UUID.randomUUID());
        otherCustomer.setCpf(newCpf);

        CustomerUpdateRequestDTO dto = new CustomerUpdateRequestDTO(
                customerId, "João da Silva", newCpf, "987.654.321-00", null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.findByCpf(newCpf)).thenReturn(Optional.of(otherCustomer));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                customerService.update(customerId, dto));

        assertTrue(exception.getMessage().contains("já existe na base de dados"));
        verify(customerRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Deve deletar customerId com sucesso")
    void testDeleteCustomerSuccess() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(true);

        customerService.deleteById(customerId);

        verify(customerRepository, times(1)).deleteById(customerId);
    }

    @Test
    @DisplayName("Deve lançar erro ao deletar customerId inexistente")
    void testDeleteCustomerNotFound() {
        UUID noExistingId = UUID.randomUUID();
        when(customerRepository.existsById(noExistingId)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                customerService.deleteById(noExistingId));

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(customerRepository, never()).deleteById(any());
    }
}