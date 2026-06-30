package io.sertaoBit.odontocore.crm.modules.financial.event;

import io.sertaoBit.odontocore.crm.core.enums.PaymentMethod;
import io.sertaoBit.odontocore.crm.core.events.DealWonEvent;
import io.sertaoBit.odontocore.crm.modules.commercial.provider.DealFinancialProvider;
import io.sertaoBit.odontocore.crm.modules.commercial.provider.DealFinancialView;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import io.sertaoBit.odontocore.crm.modules.financial.repository.InstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.EXPECTED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstallmentEventListener - Testes Unitários")
class InstallmentEventListenerTest {

    private InstallmentEventListener listener;

    @Mock private InstallmentRepository installmentRepository;
    @Mock private DealFinancialProvider dealFinancialProvider;

    @Captor private ArgumentCaptor<List<Installment>> captor;

    @BeforeEach
    void setUp() {
        listener = new InstallmentEventListener(installmentRepository, dealFinancialProvider);
    }

    private DealWonEvent event(UUID dealId) {
        return DealWonEvent.builder()
                .clinicId(UUID.randomUUID())
                .dealId(dealId)
                .ticketId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerName("Cliente")
                .evaluatorId(UUID.randomUUID())
                .closedAt(LocalDateTime.now())
                .procedures(List.of())
                .build();
    }

    private DealFinancialView view(UUID dealId, String total, int n) {
        return new DealFinancialView(
                dealId, new BigDecimal(total), PaymentMethod.INSTALLMENT, n, LocalDateTime.now());
    }

    private BigDecimal sum(List<Installment> parcelas) {
        return parcelas.stream()
                .map(Installment::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("onDealWon - resto da divisão vai na última parcela e a soma fecha o total")
    void onDealWon_remainderOnLast() {
        UUID dealId = UUID.randomUUID();
        when(dealFinancialProvider.resolveById(dealId)).thenReturn(view(dealId, "1000", 3));

        listener.onDealWon(event(dealId));

        verify(installmentRepository).saveAll(captor.capture());
        List<Installment> saved = captor.getValue();

        assertEquals(3, saved.size());
        assertEquals(0, new BigDecimal("333.33").compareTo(saved.get(0).getExpectedAmount()));
        assertEquals(0, new BigDecimal("333.33").compareTo(saved.get(1).getExpectedAmount()));
        assertEquals(0, new BigDecimal("333.34").compareTo(saved.get(2).getExpectedAmount()));
        assertEquals(0, new BigDecimal("1000").compareTo(sum(saved)));
    }

    @Test
    @DisplayName("onDealWon - quando o arredondamento sobra, a última parcela absorve o excesso")
    void onDealWon_surplusOnLast() {
        UUID dealId = UUID.randomUUID();
        when(dealFinancialProvider.resolveById(dealId)).thenReturn(view(dealId, "100", 6));

        listener.onDealWon(event(dealId));

        verify(installmentRepository).saveAll(captor.capture());
        List<Installment> saved = captor.getValue();

        assertEquals(6, saved.size());
        assertEquals(0, new BigDecimal("16.67").compareTo(saved.get(0).getExpectedAmount()));
        assertEquals(0, new BigDecimal("16.65").compareTo(saved.get(5).getExpectedAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(sum(saved)));
    }

    @Test
    @DisplayName("onDealWon - numera sequence 1..N, seta totalInstallments, status EXPECTED e paidAt nulo")
    void onDealWon_metadata() {
        UUID dealId = UUID.randomUUID();
        when(dealFinancialProvider.resolveById(dealId)).thenReturn(view(dealId, "900", 3));

        listener.onDealWon(event(dealId));

        verify(installmentRepository).saveAll(captor.capture());
        List<Installment> saved = captor.getValue();

        for (int i = 0; i < 3; i++) {
            Installment parcela = saved.get(i);
            assertEquals(i + 1, parcela.getSequence());
            assertEquals(3, parcela.getTotalInstallments());
            assertEquals(EXPECTED, parcela.getStatus());
            assertNull(parcela.getPaidAt());
        }
    }

    @Test
    @DisplayName("onDealWon - escalona dueDate mensalmente a partir de hoje")
    void onDealWon_monthlyDueDate() {
        UUID dealId = UUID.randomUUID();
        when(dealFinancialProvider.resolveById(dealId)).thenReturn(view(dealId, "300", 3));

        listener.onDealWon(event(dealId));

        verify(installmentRepository).saveAll(captor.capture());
        List<Installment> saved = captor.getValue();

        LocalDate base = LocalDate.now();
        assertEquals(base, saved.get(0).getDueDate());
        assertEquals(base.plusMonths(1), saved.get(1).getDueDate());
        assertEquals(base.plusMonths(2), saved.get(2).getDueDate());
    }

    @Test
    @DisplayName("onDealWon - à vista (N=1) gera 1 parcela com o valor total")
    void onDealWon_single() {
        UUID dealId = UUID.randomUUID();
        when(dealFinancialProvider.resolveById(dealId)).thenReturn(view(dealId, "500", 1));

        listener.onDealWon(event(dealId));

        verify(installmentRepository).saveAll(captor.capture());
        List<Installment> saved = captor.getValue();

        assertEquals(1, saved.size());
        assertEquals(1, saved.get(0).getSequence());
        assertEquals(0, new BigDecimal("500").compareTo(saved.get(0).getExpectedAmount()));
    }
}
