package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Deal;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.IDealMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.IDealRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.impl.DealServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DealServiceTest - Testes Unitários do Serviço de Deals")
class DealServiceTest {

    // A classe a ser testada NUNCA é mockada.
    private DealServiceImpl dealService;

    // Mockamos apenas as dependências.
    @Mock
    private IDealRepository dealRepository;
    @Mock
    private ICustomerRepository customerRepository;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private IDealMapper dealMapper;

    private Customer customer;
    private User user;
    private Deal deal;

    @BeforeEach
    void setUp() {
        // Instanciamos a classe real com suas dependências mockadas.
        dealService = new DealServiceImpl(
                dealRepository,
                userRepository,
                customerRepository,
                dealMapper
        );

        // Criamos objetos de dados consistentes para os testes.
        UUID customerId = UUID.randomUUID();
        customer = new Customer();
        customer.setId(customerId);

        UUID userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);

        UUID dealId = UUID.randomUUID();
        deal = new Deal();
        deal.setId(dealId);
        deal.setCustomer(customer);
        deal.setDealStatus(DealStatus.NEGOTIATING);
        deal.setDescription("Test Deal");
        deal.setProcedures(Set.of("procedure1"));
        deal.setNegotiationValue(BigDecimal.valueOf(5000));
        deal.setTargetDate(LocalDateTime.now().plusDays(10));
        deal.setClosedBy(user);
        deal.setClosedDate(null);
    }

    @Test
    @DisplayName("Deve criar um deal com sucesso")
    void testCreateDealSuccess() {
        // Arrange
        DealCreateRequestDTO createDTO = new DealCreateRequestDTO(
                customer,
                DealStatus.NEGOTIATING,
                Set.of("procedure1"),
                BigDecimal.valueOf(5000),
                user,
                "A new deal"
        );

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(dealMapper.toEntity(createDTO)).thenReturn(deal);
        when(dealRepository.save(any(Deal.class))).thenReturn(deal);

        DealResponseDTO responseDTO = new DealResponseDTO(
                        deal.getId(), deal.getCustomer(), deal.getDealStatus(), deal.getProcedures(),
                deal.getNegotiationValue(),deal.getClosedBy(), deal.getDescription(),
                        deal.getClosedDate(), deal.getTargetDate()
                );
        when(dealMapper.toResponseDTO(deal)).thenReturn(responseDTO);

        // Act
        DealResponseDTO result = dealService.create(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals(deal.getId(), result.id());
        verify(dealRepository, times(1)).save(any(Deal.class));
    }

    @Test
    @DisplayName("Deve buscar deals por status com sucesso")
    void testFindByStatusSuccess() {
        // Arrange
        deal.setDealStatus(DealStatus.WON);
        // O serviço usa findAll(), então mockamos o findAll() para retornar uma lista com o nosso deal.
        when(dealRepository.findAll()).thenReturn(List.of(deal));

        DealResponseDTO responseDTO = new DealResponseDTO(
                deal.getId(), deal.getCustomer(), deal.getDealStatus(), deal.getProcedures(),
                deal.getNegotiationValue(), deal.getClosedBy(), deal.getDescription(),
                deal.getClosedDate(), deal.getTargetDate()
        );
        when(dealMapper.toResponseDTO(deal)).thenReturn(responseDTO);

        // Act
        List<DealResponseDTO> results = dealService.findByStatus(DealStatus.WON);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(DealStatus.WON, results.get(0).dealStatus());
        // Verificamos que o método findAll() foi chamado, como esperado pela implementação do serviço.
        verify(dealRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve deletar um deal com sucesso")
    void testDeleteDealSuccess() {
        // Arrange
        deal.setDealStatus(DealStatus.NEGOTIATING);
        when(dealRepository.findById(deal.getId())).thenReturn(Optional.of(deal));
        doNothing().when(dealRepository).deleteById(deal.getId());

        // Act
        assertDoesNotThrow(() -> dealService.delete(deal.getId()));

        // Assert
        verify(dealRepository, times(1)).deleteById(deal.getId());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar um deal ganho (WON)")
    void testDeleteWonDealThrowsException() {
        // Arrange
        deal.setDealStatus(DealStatus.WON);
        when(dealRepository.findById(deal.getId())).thenReturn(Optional.of(deal));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dealService.delete(deal.getId());
        });

        assertTrue(exception.getMessage().contains("Cannot delete Deal in terminal status"));
        verify(dealRepository, never()).deleteById(any());
    }
}
