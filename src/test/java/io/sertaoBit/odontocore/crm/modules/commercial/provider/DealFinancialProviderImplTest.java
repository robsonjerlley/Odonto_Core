package io.sertaoBit.odontocore.crm.modules.commercial.provider;

import io.sertaoBit.odontocore.crm.core.enums.PaymentMethod;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DealFinancialProvider - Testes Unitários")
class DealFinancialProviderImplTest {

    private DealFinancialProvider provider;

    @Mock private DealRepository dealRepository;

    @BeforeEach
    void setUp() {
        provider = new DealFinancialProviderImpl(dealRepository);
    }

    private Deal deal(UUID id, String total, String finalValue, Integer count) {
        return Deal.builder()
                .id(id)
                .totalValue(new BigDecimal(total))
                .finalValue(finalValue == null ? null : new BigDecimal(finalValue))
                .installmentCount(count)
                .paymentMethod(PaymentMethod.INSTALLMENT)
                .closedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("resolveById - usa finalValue como expectedAmount quando há desconto")
    void resolveById_usesFinalValue() {
        UUID id = UUID.randomUUID();
        when(dealRepository.findById(id)).thenReturn(Optional.of(deal(id, "1000", "800", 4)));

        DealFinancialView view = provider.resolveById(id);

        assertEquals(id, view.dealId());
        assertEquals(0, new BigDecimal("800").compareTo(view.expectedAmount()));
        assertEquals(4, view.installmentCount());
        assertEquals(PaymentMethod.INSTALLMENT, view.paymentMethod());
    }

    @Test
    @DisplayName("resolveById - cai para totalValue quando finalValue é nulo (sem desconto)")
    void resolveById_fallsBackToTotal() {
        UUID id = UUID.randomUUID();
        when(dealRepository.findById(id)).thenReturn(Optional.of(deal(id, "1000", null, 1)));

        DealFinancialView view = provider.resolveById(id);

        assertEquals(0, new BigDecimal("1000").compareTo(view.expectedAmount()));
    }

    @Test
    @DisplayName("resolveById - lança ResourceNotFoundException quando o deal não existe")
    void resolveById_notFound() {
        UUID id = UUID.randomUUID();
        when(dealRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> provider.resolveById(id));
    }
}
