package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DealItemRequestDTO(

        @NotNull UUID procedureId,
        BigDecimal priceOverride,
        @Min(1) int quantity,
        String note

) {
}
