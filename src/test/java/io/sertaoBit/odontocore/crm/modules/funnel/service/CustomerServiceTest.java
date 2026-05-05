
package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Department;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.ICustomerMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.IDepartmentRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ITicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.CustomerServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceTest - Testes Unitários do Serviço")
class CustomerServiceTest {

    private CustomerServiceImpl customerService;

    @Mock
    private ICustomerRepository customerRepository;

    @Mock
    private IDepartmentRepository departmentRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private ICustomerMapper customerMapper;

    private ITicketRepository  ticketRepository;

    private UUID customerId;
    private UUID departmentId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(
                customerRepository,
                customerMapper,
                userRepository,
                departmentRepository,
                ticketRepository

        );

        customerId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Deve criar customer com sucesso")
    void testCreateCustomerSuccess() {
        // Arrange
        Department department = new Department();
        department.setId(departmentId);

        User user = new User();
        user.setId(userId);

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("João Silva");

        CustomerResponseDTO responseDTO = mock(CustomerResponseDTO.class);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(customerMapper.toEntity(any())).thenReturn(customer);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(customerMapper.toResponseDTO(customer)).thenReturn(responseDTO);

        CustomerCreateRequestDTO dto = new CustomerCreateRequestDTO(
                "João Silva", "123.456.789-00", "11999999999",
                "São Paulo", "Rua A, 123", "Cliente importante",
                TicketStatus.TICKET_OPEN, departmentId
        );

        // Act
        CustomerResponseDTO result = customerService.create(dto);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.id());
        assertEquals("João Silva", result.name());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando department não encontrado")
    void testCreateCustomerDepartmentNotFound() {
        // Arrange
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        CustomerCreateRequestDTO dto = mock(CustomerCreateRequestDTO.class);


        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.create(dto);
        });

        assertTrue(exception.getMessage().contains("Department not found"));
        verify(customerRepository, never()).save(any());
    }

    // ========== FIND TESTS ==========

    @Test
    @DisplayName("Deve buscar customer por ID com sucesso")
    void testFindByIdSuccess() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setName("João Silva");

        CustomerResponseDTO responseDTO = mock(CustomerResponseDTO.class);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponseDTO(customer)).thenReturn(responseDTO);

        // Act
        CustomerResponseDTO result = customerService.findById(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.id());
        assertEquals("João Silva", result.name());
        verify(customerRepository, times(1)).findById(customerId);
    }

    @Test
    @DisplayName("Deve lançar erro quando customer não encontrado por ID")
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
    @DisplayName("Deve buscar customer por CPF com sucesso")
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
    @DisplayName("Deve atualizar customer com sucesso")
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
    @DisplayName("Deve impedir atualizar customer com CPF duplicado")
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
    @DisplayName("Deve deletar customer com sucesso")
    void testDeleteCustomerSuccess() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(true);

        // Act
        customerService.deleteById(customerId);

        // Assert
        verify(customerRepository, times(1)).deleteById(customerId);
    }

    @Test
    @DisplayName("Deve lançar erro ao deletar customer inexistente")
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
}

