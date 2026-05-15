
package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.CustomerSource;
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
    @DisplayName("Deve buscar customerId por ID com sucesso")
    void create() {

        // Arrange
        UUID userId = UUID.randomUUID();

        CustomerCreateRequestDTO dto = new CustomerCreateRequestDTO(
                "Jão da Silva",
                "123456789",
                "email",
                "12385698741",
                ADS_PAID,
                INSTAGRAM,
                "Um novo sorriso",
                securityUtils.getCurrentUserId()
        );

        // Act

         CustomerResponseDTO   result  =  customerService.create(dto);

        // Assert
        assertNotNull(result);
        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(securityUtils, times(1)).getCurrentUserId();
    }
    /*
    @Test
    @DisplayName("Deve lançar erro quando customerId não encontrado por ID")
    void testFindByIdNotFound() {
        // Arrange
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
        String cpf = "123.456.789-00";
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCpf(cpf);

        CustomerResponseDTO responseDTO = mock(CustomerResponseDTO.class);


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
        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setCpf(cpf);
        existingCustomer.setName("João Silva");

        CustomerUpdateRequestDTO dto = mock(CustomerUpdateRequestDTO.class);

        CustomerResponseDTO responseDTO = mock(CustomerResponseDTO.class);

        when(customerRepository.findByCpf(cpf)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(existingCustomer);
        when(customerMapper.toResponseDTO(existingCustomer)).thenReturn(responseDTO);

        // Act
        CustomerResponseDTO result = customerService.update(cpf, dto);

        // Assert
        assertNotNull(result);
        assertEquals("João Silva Atualizado", result.name());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Deve impedir atualizar customerId com CPF duplicado")
    void testUpdateCustomerDuplicateCPF() {
        // Arrange
        String oldCpf = "123.456.789-00";
        String newCpf = "987.654.321-00";

        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setCpf(oldCpf);

        Customer otherCustomer = new Customer();
        otherCustomer.setId(UUID.randomUUID());
        otherCustomer.setCpf(newCpf);

        CustomerUpdateRequestDTO dto = mock(CustomerUpdateRequestDTO.class);

        when(customerRepository.findByCpf(oldCpf)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.findByCpf(newCpf)).thenReturn(Optional.of(otherCustomer));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.update(oldCpf, dto);
        });

        assertTrue(exception.getMessage().contains("já pertence a outro cliente"));
        verify(customerRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Deve deletar customerId com sucesso")
    void testDeleteCustomerSuccess() {
        // Arrange
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
        when(customerRepository.existsById(customerId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.deleteById(customerId);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(customerRepository, never()).deleteById(any());
    }
*/

}

