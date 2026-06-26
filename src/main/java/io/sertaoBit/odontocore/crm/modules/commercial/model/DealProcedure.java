package io.sertaoBit.odontocore.crm.modules.commercial.model;


import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record DealProcedure(
        UUID procedureId,
        String name,
        String code,
        BigDecimal tableValue,
        BigDecimal priceOverride,
        int quantity,
        String note
) {
}
