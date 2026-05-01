package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.deal.DealCreateRequestDTO;
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
import java.time.LocalDateTime;
import java.util.Collections;
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

    @Mock
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
    private Deal deal;
    private DealResponseDTO dealResponseDTO;
    private Customer customer;
    private User user;

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

        customer = new Customer();
        customer.setId(customerId);

        user = new User();
        user.setId(userId);

        deal = new Deal();
        deal.setId(dealId);
        deal.setCustomer(customer);
        deal.setClosedBy(user);
        deal.setDealStatus(DealStatus.NEGOTIATING);
        deal.setNegotiationValue(BigDecimal.valueOf(5000));
        deal.setDescription("Test Deal");
        deal.setProcedures(Set.of("procedure1", "procedure2"));
        deal.setClosedDate(LocalDateTime.now());

        dealResponseDTO = new DealResponseDTO(
                deal.getId(dealId),

        );
    }

    // ========== CREATE TESTS ==========

    @Test
    @DisplayName("Deve criar deal com sucesso")
    void testCreateDealSuccess() {
        // Arrange
        DealCreateRequestDTO createDTO = new DealCreateRequestDTO(
                customerId,
                "Deal description",
                BigDecimal.valueOf(5000),
                Set.of("procedure1", "procedure2")
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(dealMapper.toEntity(any(DealCreateRequestDTO.class))).thenReturn(deal);
        when(dealRepository.save(any(Deal.class))).thenReturn(deal);
        when(dealMapper.toResponseDTO(any(Deal.class))).thenReturn(dealResponseDTO);

        // Act
        DealResponseDTO result = dealService.create(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals(dealId, result.id());
        assertEquals(DealStatus.NEGOTIATING, result.dealStatus());
        verify(customerRepository, times(1)).findById(customerId);
        verify(dealRepository, times(1)).save(any(Deal.class));
    }

    @Test
    @DisplayName("Deve lançar erro quando customer não encontrado na criação")
    void testCreateDealCustomerNotFound() {
        // Arrange
        DealCreateRequestDTO createDTO = new DealCreateRequestDTO(
                customerId, "Deal", BigDecimal.valueOf(5000), Set.of()
        );
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> dealService.create(createDTO));
        assertTrue(exception.getMessage().contains("Customer Not Found"));
        verify(dealRepository, never()).save(any());
    }

    // ========== FIND TESTS ==========

    @Test
    @DisplayName("Deve buscar deal por ID com sucesso")
    void testFindByIdSuccess() {
        // Arrange
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealMapper.toResponseDTO(deal)).thenReturn(dealResponseDTO);

        // Act
        DealResponseDTO result = dealService.findById(dealId);

        // Assert
        assertNotNull(result);
        assertEquals(dealId, result.id());
        verify(dealRepository, times(1)).findById(dealId);
    }
    
    @Test
    @DisplayName("Deve lançar erro quando deal não encontrado por ID")
    void testFindByIdNotFound() {
        // Arrange
        when(dealRepository.findById(dealId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> dealService.findById(dealId));
    }

    @Test
    @DisplayName("Deve buscar deals por status com sucesso")
    void testFindByStatusSuccess() {
        // Arrange
        deal.setDealStatus(DealStatus.WON);
        when(dealRepository.findByDealStatus(DealStatus.WON)).thenReturn(List.of(deal));
        when(dealMapper.toResponseDTO(deal)).thenReturn(dealResponseDTO);

        // Act
        List<DealResponseDTO> results = dealService.findByStatus(DealStatus.WON);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(dealRepository, times(1)).findByDealStatus(DealStatus.WON);
    }

    // ========== UPDATE STATUS TESTS ==========

    @Test
    @DisplayName("Deve atualizar status do deal com sucesso")
    void testUpdateStatusSuccess() {
        // Arrange
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealRepository.save(any(Deal.class))).thenReturn(deal);
        when(dealMapper.toResponseDTO(any(Deal.class))).thenReturn(dealResponseDTO);

        // Act
        DealResponseDTO result = dealService.updateStatus(dealId, DealStatus.WON);

        // Assert
        assertNotNull(result);
        assertEquals(dealResponseDTO, result);
        verify(dealRepository, times(1)).save(any(Deal.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao atualizar status com null")
    void testUpdateStatusWithNull() {
        // Arrange
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> dealService.updateStatus(dealId, null));
        assertTrue(exception.getMessage().contains("cannot not be null"));
        verify(dealRepository, never()).save(any());
    }

    // ========== DELETE TESTS ==========

    @Test
    @DisplayName("Deve deletar deal com sucesso")
    void testDeleteDealSuccess() {
        // Arrange
        when(dealRepository.existsById(dealId)).thenReturn(true);
        doNothing().when(dealRepository).deleteById(dealId);

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
        RuntimeException exception = assertThrows(RuntimeException.class, () -> dealService.delete(dealId));
        assertTrue(exception.getMessage().contains("not found"));
        verify(dealRepository, never()).deleteById(any());
    }

    // ========== FIND BY CUSTOMER TESTS ==========

    @Test
    @DisplayName("Deve buscar deals por customer ID com sucesso")
    void testFindByCustomerSuccess() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(dealRepository.findByCustomerId(customerId)).thenReturn(List.of(deal));
        when(dealMapper.toResponseDTO(deal)).thenReturn(dealResponseDTO);

        // Act
        List<DealResponseDTO> results = dealService.findByCustomer(customerId);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(dealRepository, times(1)).findByCustomerId(customerId);
    }
    
    @Test
    @DisplayName("Deve retornar lista vazia se cliente não tiver deals")
    void testFindByCustomerNoDeals() {
        // Arrange
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(dealRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        // Act
        List<DealResponseDTO> results = dealService.findByCustomer(customerId);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(dealRepository, times(1)).findByCustomerId(customerId);
    }


    // ========== FIND BY DATE RANGE TESTS ==========

    @Test
    @DisplayName("Deve buscar deals por intervalo de datas com sucesso")
    void testFindByDateRangeSuccess() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        deal.setClosedDate(LocalDateTime.now());
        
        when(dealRepository.findByClosedDateBetween(start, end)).thenReturn(List.of(deal));
        when(dealMapper.toResponseDTO(deal)).thenReturn(dealResponseDTO);

        // Act
        List<DealResponseDTO> results = dealService.findByDateRange(start, end);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(dealRepository, times(1)).findByClosedDateBetween(start, end);
    }
}
