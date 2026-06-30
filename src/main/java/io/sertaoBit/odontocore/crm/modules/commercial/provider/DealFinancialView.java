package io.sertaoBit.odontocore.crm.modules.commercial.provider;

import io.sertaoBit.odontocore.crm.core.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DealFinancialView(
        UUID dealId,
        BigDecimal expectedAmount,
        PaymentMethod paymentMethod,
        int installmentCount,
        LocalDateTime closedAt
) {
}
