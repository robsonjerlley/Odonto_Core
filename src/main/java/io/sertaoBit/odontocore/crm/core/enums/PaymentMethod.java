package io.sertaoBit.odontocore.crm.core.enums;

import java.math.BigDecimal;

public enum PaymentMethod {

    PIX(new BigDecimal("1.00")),
    CASH(new BigDecimal("1.00")),
    DEBIT_CARD(new BigDecimal("0.98")),
    CREDIT_CARD(new BigDecimal("0.97")),
    INSTALLMENT(new BigDecimal("0.85")),
    DENTAL_INSURANCE(new BigDecimal("0.90"));

    private final BigDecimal conversionFactor;

    PaymentMethod(BigDecimal conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    public BigDecimal getConversionFactor() {
        return conversionFactor;
    }
}
