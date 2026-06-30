package io.sertaoBit.odontocore.crm.modules.financial.api.dto.request;


import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayRequestDTO(
        @NotNull BigDecimal paidAmount,
        @NotNull LocalDate paidAt
) {
}
