package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyDiscountRequestDTO(
        @NotNull BigDecimal discountPct
) {
}
