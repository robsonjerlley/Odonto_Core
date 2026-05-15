
package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.CustomerMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private SecurityUtils securityUtils;


    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(
                customerRepository,
                customerMapper,
                securityUtils
        );
    }

    // ========== CREATE TESTS ==========


    @Test
    @DisplayName("Deve criar customer com sucesso")
    void create() {

        // Arrange
        UUID userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        CustomerCreateRequestDTO dto = new CustomerCreateRequestDTO(
                "Jão da Silva",
                "123456789",
                "83999999",
                "mail",
                ADS_PAID,
                INSTAGRAM,
                "Um novo sorriso",
                null
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
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(customer);


        CustomerResponseDTO expectedDTO = new CustomerResponseDTO(
                customer.getId(),
                customer.getName(),
                customer.getCpf(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getSource(),
                customer.getAdChannel(),
                customer.getAdCampaign(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getCreatedBy(),
                null
        );

        when(customerMapper.toResponseDTO(customer)).thenReturn(expectedDTO);
        // Act
        CustomerResponseDTO result = customerService.create(dto);


        // Assert
        assertNotNull(result);
        assertEquals(dto.name(), result.name());
        assertEquals(dto.cpf(), result.cpf());
        assertEquals(userId, result.createdBy());


        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(securityUtils, times(1)).getCurrentUserId();
        verify(customerMapper, times(1)).toResponseDTO(customer);
    }

    @Test
    @DisplayName("Deve lançar erro quando customerId não encontrado por ID")
    void testFindByIdNotFound() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.findById(customerId);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
    }

    @Test
    @DisplayName("Deve buscar customerId por CPF com sucesso")
    void testFindByCpfSuccess() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        String cpf = "123.456.789-00";

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCpf(cpf);


        CustomerResponseDTO responseDTO = new CustomerResponseDTO(
                customerId, null, cpf, null,
                null, null, null,
                null, null, null, null,
                null


        );


        when(customerRepository.findByCpf(cpf)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponseDTO(customer)).thenReturn(responseDTO);

        // Act
        CustomerResponseDTO result = customerService.findByCpf(cpf);

        // Assert
        assertNotNull(result);

        assertEquals(cpf, result.cpf());
        verify(customerRepository, times(1)).findByCpf(cpf);
    }

    // ========== UPDATE TESTS ==========

    @Test
    @DisplayName("Deve atualizar customerId com sucesso")
    void testUpdateCustomerSuccess() {
        // Arrange
        String cpf = "123.456.789-00";
        UUID customerId = UUID.randomUUID();

        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setName("João Siva");
        existingCustomer.setCpf(cpf);
        existingCustomer.setPhone("8399875878");


        CustomerUpdateRequestDTO dto = new CustomerUpdateRequestDTO(
                customerId, "João da Silva", cpf,
                "8399875878", null
        );


        CustomerResponseDTO responseDTO = new CustomerResponseDTO(
                dto.id(), dto.name(), dto.cpf(), dto.phone(),
                null, null, null,
                null, null, null, null,
                null


        );


        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(existingCustomer);

        when(customerMapper.toResponseDTO(existingCustomer)).thenReturn(responseDTO);

        // Act
        CustomerResponseDTO result = customerService.update(customerId, dto);

        // Assert
        assertNotNull(result);
        assertEquals("João da Silva", result.name());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Deve impedir atualizar customerId com CPF duplicado")
    void testUpdateCustomerDuplicateCPF() {
        // Arrange
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
        otherCustomer.setName("Antonio Alves");
        otherCustomer.setPhone("839995875");

        CustomerUpdateRequestDTO dto = new CustomerUpdateRequestDTO(
                customerId, "João da Silva", newCpf,
                "987.654.321-00", null
        );
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.findByCpf(otherCustomer.getCpf())).thenReturn(Optional.of(otherCustomer));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.update(customerId, dto);
        });

        assertTrue(exception.getMessage().contains("já existe na base de dados"));
        verify(customerRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Deve deletar customerId com sucesso")
    void testDeleteCustomerSuccess() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        when(customerRepository.existsById(customerId)).thenReturn(true);

        // Act
        customerService.deleteById(customerId);

        // Assert
        verify(customerRepository, times(1)).deleteById(customerId);
    }

    @Test
    @DisplayName("Deve lançar erro ao deletar customerId inexistente")
    void testDeleteCustomerNotFound() {
        // Arrange
        UUID noExistingId = UUID.randomUUID();
        when(customerRepository.existsById(noExistingId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.deleteById(noExistingId);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(customerRepository, never()).deleteById(any());
    }


}

