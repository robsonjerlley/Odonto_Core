package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.deal.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.DealStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DealServiceTest - Testes Unitários do Serviço de Deals")
class DealServiceTest {

    private DealServiceImpl dealService;

    @Mock
    private IDealRepository dealRepository;

    @Mock
    private ICustomerRepository customerRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IDealMapper dealMapper;

    private UUID dealId;
    private UUID customerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        dealService = new DealServiceImpl(
                dealRepository,
                userRepository,
                customerRepository,
                dealMapper
        );

        dealId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Deve criar deal com sucesso")
    void testCreateDealSuccess() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);

        Deal deal = new Deal();
        deal.setId(dealId);
        deal.setCustomer(customer);
        deal.setDealStatus(DealStatus.NEGOTIATING);

        DealResponseDTO responseDTO = new DealResponseDTO(
                dealId, null, DealStatus.NEGOTIATING, BigDecimal.valueOf(5000), null, null, null, null
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(dealMapper.toEntity(any())).thenReturn(deal);
        when(dealRepository.save(any(Deal.class))).thenReturn(deal);
        when(dealMapper.toResponseDTO(deal)).thenReturn(responseDTO);

        DealCreateRequestDTO dto = new DealCreateRequestDTO(
                new Object() { public UUID getId() { return customerId; } },
                "Deal description",
                BigDecimal.valueOf(5000),
                Set.of("procedure1", "procedure2")
        );

        // Act
        DealResponseDTO result = dealService.create(dto);

        // Assert
        assertNotNull(result);
        assertEquals(dealId, result.id());
        assertEquals(DealStatus.NEGOTIATING, result.dealStatus());
        verify(customerRepository, times(1)).findById(customerId);
        verify(dealRepository, times(1)).save(any(Deal.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando customer não encontrado")
    void testCreateDealCustomerNotFound() {
        // Arrange
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        DealCreateRequestDTO dto = new DealCreateRequestDTO(
                new Object() { public UUID getId() { return customerId; } },
                "Deal",
                BigDecimal.valueOf(5000),
                Set.of()
        );

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dealService.create(dto);
        });

        assertTrue(exception.getMessage().contains("Customer Not Found"));
        verify(dealRepository, never()).save(any());
    }

    // ========== FIND TESTS ==========

    @Test
    @DisplayName("Deve buscar deal por ID com sucesso")
    void testFindByIdSuccess() {
        // Arrange
        Deal deal = new Deal();
        deal.setId(dealId);
        deal.setDealStatus(DealStatus.NEGOTIATING);

        DealResponseDTO responseDTO = new DealResponseDTO(
                dealId, null, DealStatus.NEGOTIATING, BigDecimal.valueOf(5000), null, null, null, null
        );

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealMapper.toResponseDTO(deal)).thenReturn(responseDTO);

        // Act
        DealResponseDTO result = dealService.findById(dealId);

        // Assert
        assertNotNull(result);
        assertEquals(dealId, result.id());
        verify(dealRepository, times(1)).findById(dealId);
    }

    @Test
    @DisplayName("Deve buscar deals por status com sucesso")
    void testFindByStatusSuccess() {
        // Arrange
        Deal deal1 = new Deal();
        deal1.setId(UUID.randomUUID());
        deal1.setDealStatus(DealStatus.WON);

        Deal deal2 = new Deal();
        deal2.setId(UUID.randomUUID());
        deal2.setDealStatus(DealStatus.WON);

        DealResponseDTO dto1 = new DealResponseDTO(
                deal1.getId(), null, DealStatus.WON, BigDecimal.valueOf(5000), null, null, null, null
        );
        DealResponseDTO dto2 = new DealResponseDTO(
                deal2.getId(), null, DealStatus.WON, BigDecimal.valueOf(7000), null, null, null, null
        );

        when(dealRepository.findAll()).thenReturn(List.of(deal1, deal2));
        when(dealMapper.toResponseDTO(deal1)).thenReturn(dto1);
        when(dealMapper.toResponseDTO(deal2)).thenReturn(dto2);

        // Act
        List<DealResponseDTO> results = dealService.findByStaus(DealStatus.WON);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(dealRepository, times(1)).findAll();
    }

    // ========== UPDATE STATUS TESTS ==========

    @Test
    @DisplayName("Deve atualizar status do deal com sucesso")
    void testUpdateStatusSuccess() {
        // Arrange
        Deal deal = new Deal();
        deal.setId(dealId);
        deal.setDealStatus(DealStatus.NEGOTIATING);

        DealResponseDTO responseDTO = new DealResponseDTO(
                dealId, null, DealStatus.WON, BigDecimal.valueOf(5000), null, null, null, null
        );

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealRepository.save(any(Deal.class))).thenReturn(deal);
        when(dealMapper.toResponseDTO(deal)).thenReturn(responseDTO);

        // Act
        DealResponseDTO result = dealService.updateStatus(dealId, DealStatus.WON);

        // Assert
        assertNotNull(result);
        assertEquals(DealStatus.WON, result.dealStatus());
        verify(dealRepository, times(1)).save(any(Deal.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao atualizar status com null")
    void testUpdateStatusWithNull() {
        // Arrange
        Deal deal = new Deal();
        deal.setId(dealId);

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dealService.updateStatus(dealId, null);
        });

        assertTrue(exception.getMessage().contains("cannot not be null"));
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Deve deletar deal com sucesso")
    void testDeleteDealSuccess() {
        // Arrange
        when(dealRepository.existsById(dealId)).thenReturn(true);

        // Act
        dealService.delete(dealId);

        // Assert
        verify(dealRepository, times(1)).deleteById(dealId);
    }

    @Test
    @DisplayName("Deve lançar erro ao deletar deal inexistente")
    void testDeleteDealNotFound() {
        // Arrange
        when(dealRepository.existsById(dealId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dealService.delete(dealId);
        });

        assertTrue(exception.getMessage().contains("not found"));
        verify(dealRepository, never()).deleteById(any());
    }

    // ========== FIND BY CUSTOMER TESTS ==========

    @Test
    @DisplayName("Deve buscar deals por customer ID com sucesso")
    void testFindByCustomerSuccess() {
        // Arrange
        Customer customer = new Customer();
        customer.setId(customerId);

        Deal deal = new Deal();
        deal.setId(dealId);
        deal.setCustomer(customer);

        DealResponseDTO responseDTO = new DealResponseDTO(
                dealId, null, DealStatus.NEGOTIATING, BigDecimal.valueOf(5000), null, null, null, null
        );

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(dealRepository.findAll()).thenReturn(List.of(deal));
        when(dealMapper.toResponseDTO(deal)).thenReturn(responseDTO);

        // Act
        List<DealResponseDTO> results = dealService.findByCustomer(customerId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(customerRepository, times(1)).existsById(customerId);
    }

    // ========== FIND BY DATE RANGE TESTS ==========

    @Test
    @DisplayName("Deve buscar deals por intervalo de datas com sucesso")
    void testFindByDateRangeSuccess() {
        // Arrange
        Deal deal = new Deal();
        deal.setId(dealId);
        deal.setClosedDate(LocalDateTime.now());

        DealResponseDTO responseDTO = new DealResponseDTO(
                dealId, null, DealStatus.WON, BigDecimal.valueOf(5000), null, null, null, null
        );

        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        when(dealRepository.findAll()).thenReturn(List.of(deal));
        when(dealMapper.toResponseDTO(deal)).thenReturn(responseDTO);

        // Act
        List<DealResponseDTO> results = dealService.findByDateRange(start, end);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(dealRepository, times(1)).findAll();
    }
}

