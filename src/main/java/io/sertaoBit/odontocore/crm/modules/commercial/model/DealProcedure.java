package io.sertaoBit.odontocore.crm.modules.commercial.model;

import java.math.BigDecimal;

public record DealProcedure(
        String name,
        String code,
        BigDecimal tableValue,
        int quantity,
        String note
) {
}
