package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.service.ICustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@DisplayName("CustomerControllerTest - Testes de Unidade do Controller")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    private ICustomerService customerService;

    private UUID customerId;
    private UUID departmentId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ========== CREATE ENDPOINT TESTS ==========

    @Test
    @DisplayName("POST /api/v1/customers/create - Deve criar customer e retornar 201")
    void testCreateCustomer() throws Exception {
        // Arrange
        CustomerCreateRequestDTO requestDTO = new CustomerCreateRequestDTO(
                "João Silva", "123.456.789-00", "11999999999",
                "São Paulo", "Rua A, 123", "Cliente importante",
                TicketStatus.TICKET_OPEN, departmentId
        );

        CustomerResponseDTO responseDTO = new CustomerResponseDTO(
                customerId,
                "João Silva",
                "123.456.789-00",
                "11999999999",
                "São Paulo",
                "Rua A, 123",
                Collections.singletonList("Cliente importante"),
                TicketStatus.TICKET_OPEN,
                userId
        );

        when(customerService.create(any(CustomerCreateRequestDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/customers/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.cpf").value("123.456.789-00"));

        verify(customerService, times(1)).create(any(CustomerCreateRequestDTO.class));
    }

    // ========== READ-ALL ENDPOINT TESTS ==========

    @Test
    @DisplayName("GET /api/v1/customers - Deve retornar lista de customers")
    void testFindAllCustomers() throws Exception {
        // Arrange
        CustomerResponseDTO dto1 = new CustomerResponseDTO(
                UUID.randomUUID(), "João", "111.111.111-11", "11111", "City1", "Address1",
                Collections.emptyList(), TicketStatus.TICKET_OPEN, UUID.randomUUID()
        );
        CustomerResponseDTO dto2 = new CustomerResponseDTO(
                UUID.randomUUID(), "Maria", "222.222.222-22", "22222", "City2", "Address2",
                Collections.emptyList(), TicketStatus.TICKET_IN_PROGRESS, UUID.randomUUID()
        );

        when(customerService.findAll()).thenReturn(List.of(dto1, dto2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("João"))
                .andExpect(jsonPath("$[1].name").value("Maria"));

        verify(customerService, times(1)).findAll();
    }

    // ========== READ-BY-ID ENDPOINT TESTS ==========

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Deve retornar customer por ID")
    void testFindByIdCustomer() throws Exception {
        // Arrange
        CustomerResponseDTO responseDTO = new CustomerResponseDTO(
                customerId, "João Silva", "123.456.789-00", "11999999999", "São Paulo",
                "Rua A, 123", Collections.emptyList(), TicketStatus.TICKET_OPEN, userId
        );

        when(customerService.findById(customerId)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.name").value("João Silva"));

        verify(customerService, times(1)).findById(customerId);
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - Deve retornar 404 quando customer não encontrado")
    void testFindByIdNotFound() throws Exception {
        // Arrange
        when(customerService.findById(any(UUID.class))).thenThrow(new RuntimeException("Customer not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(customerService, times(1)).findById(any(UUID.class));
    }

    // ========== FIND BY NAME ENDPOINT TESTS ==========

    @Test
    @DisplayName("GET /api/v1/customers/name/{name} - Deve retornar customers por nome")
    void testFindByNameCustomer() throws Exception {
        // Arrange
        String name = "João";
        CustomerResponseDTO dto = new CustomerResponseDTO(
                customerId, "João Silva", "123.456.789-00", "11999999999", "São Paulo",
                "Rua A, 123", Collections.emptyList(), TicketStatus.TICKET_OPEN, userId
        );

        when(customerService.findByName(name)).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/name/" + name)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("João Silva"));

        verify(customerService, times(1)).findByName(name);
    }

    // ========== FIND BY CPF ENDPOINT TESTS ==========

    @Test
    @DisplayName("GET /api/v1/customers/cpf/{cpf} - Deve retornar customer por CPF")
    void testFindByCpfCustomer() throws Exception {
        // Arrange
        String cpf = "123.456.789-00";
        CustomerResponseDTO responseDTO = new CustomerResponseDTO(
                customerId, "João Silva", cpf, "11999999999", "São Paulo",
                "Rua A, 123", Collections.emptyList(), TicketStatus.TICKET_OPEN, userId
        );

        when(customerService.findByCpf(cpf)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customers/cpf/" + cpf)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(cpf))
                .andExpect(jsonPath("$.name").value("João Silva"));

        verify(customerService, times(1)).findByCpf(cpf);
    }

    // ========== DELETE ENDPOINT TESTS ==========

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - Deve deletar customer e retornar 204")
    void testDeleteCustomer() throws Exception {
        // Arrange
        doNothing().when(customerService).deleteById(customerId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/customers/" + customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(customerService, times(1)).deleteById(customerId);
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @DisplayName("POST /api/v1/customers/create - Deve retornar 400 para validação falha")
    void testCreateCustomerValidationError() throws Exception {
        // Arrange - DTO inválido (name em branco)
        CustomerCreateRequestDTO requestDTO = new CustomerCreateRequestDTO(
                "", "123.456.789-00", "11999999999",
                "São Paulo", "Rua A, 123", "Cliente importante",
                TicketStatus.TICKET_OPEN, departmentId
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/customers/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).create(any());
    }
}
