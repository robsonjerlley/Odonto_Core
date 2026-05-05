package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.AbstractIntegrationTest;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomerControllerIT extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Deve criar um novo customer com sucesso")
    void shouldCreateCustomerSuccessfully() {
        // Arrange
        CustomerCreateRequestDTO requestDTO = new CustomerCreateRequestDTO(
                "Cliente Teste",
                "123.456.789-01",
                "11999998888",
                "Cidade Teste",
                "Endereço Teste",
                "Notas Teste",
                TicketStatus.TICKET_OPEN,
                null // Para este teste, não vamos associar a um departamento
        );

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/customers/create")
                .bodyValue(requestDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponseDTO.class)
                .value(response -> {
                    assertThat(response.id()).isNotNull();
                    assertThat(response.name()).isEqualTo("Cliente Teste");
                    assertThat(response.cpf()).isEqualTo("123.456.789-01");
                });
    }
}
