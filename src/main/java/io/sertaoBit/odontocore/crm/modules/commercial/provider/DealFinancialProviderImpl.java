package io.sertaoBit.odontocore.crm.modules.commercial.provider;

import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DealFinancialProviderImpl implements DealFinancialProvider {

    private final DealRepository dealRepository;

    public DealFinancialProviderImpl(DealRepository dealRepository) {
        this.dealRepository = dealRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DealFinancialView resolveById(UUID dealId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + dealId));

        BigDecimal expected = deal.getFinalValue() != null ? deal.getFinalValue() : deal.getTotalValue();
        return new DealFinancialView(
                deal.getId(),
                expected,
                deal.getPaymentMethod(),
                deal.getInstallmentCount(),
                deal.getClosedAt()
        );
    }
}
