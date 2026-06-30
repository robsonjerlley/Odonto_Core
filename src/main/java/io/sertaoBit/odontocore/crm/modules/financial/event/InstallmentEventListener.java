package io.sertaoBit.odontocore.crm.modules.financial.event;

import io.sertaoBit.odontocore.crm.core.events.DealWonEvent;
import io.sertaoBit.odontocore.crm.modules.commercial.provider.DealFinancialProvider;
import io.sertaoBit.odontocore.crm.modules.commercial.provider.DealFinancialView;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import io.sertaoBit.odontocore.crm.modules.financial.repository.InstallmentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.EXPECTED;
import static java.math.RoundingMode.HALF_EVEN;

@Component
public class InstallmentEventListener {

    private final InstallmentRepository installmentRepository;
    private final DealFinancialProvider dealFinancialProvider;

    public InstallmentEventListener(
            InstallmentRepository installmentRepository,
            DealFinancialProvider dealFinancialProvider
    ) {
        this.installmentRepository = installmentRepository;
        this.dealFinancialProvider = dealFinancialProvider;
    }

    @EventListener
    public void onDealWon(DealWonEvent event) {

        DealFinancialView fin = dealFinancialProvider.resolveById(event.dealId());
        int n = fin.installmentCount();
        BigDecimal each = fin.expectedAmount().divide(BigDecimal.valueOf(n), 2, HALF_EVEN);

        List<Installment> installments = new ArrayList<Installment>();
        for (int i = 1; i <= n; i++) {
            Installment installment = Installment.builder()
                    .dealId(event.dealId())
                    .customerId(event.customerId())
                    .customerName(event.customerName())
                    .sequence(i)
                    .totalInstallments(n)
                    .expectedAmount(i < n ? each :
                            fin.expectedAmount().subtract(each.multiply(BigDecimal.valueOf(n - 1))))
                    .dueDate(LocalDate.now().plusMonths(i - 1))
                    .status(EXPECTED)
                    .paidAt(null)
                    .build();
            installments.add(installment);
        }

        installmentRepository.saveAll(installments);
    }
}

