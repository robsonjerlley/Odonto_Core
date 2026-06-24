package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.deal;

import java.math.BigDecimal;
import java.util.UUID;

public record DealProcedureResponseDTO(

        UUID procedureId,
        String name,
        String code,
        BigDecimal tableValue,
        BigDecimal priceOverride,
        int quantity,
        BigDecimal effectivePrice,
        BigDecimal subtotal,
        String note


) {
}
