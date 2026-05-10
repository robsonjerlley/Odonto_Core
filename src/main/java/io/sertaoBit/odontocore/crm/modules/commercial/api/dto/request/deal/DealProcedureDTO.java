package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DealProcedureDTO(

        @NotBlank String name,
        String code,
        @NotNull BigDecimal tableValue,
        @Min(1) int quantity,
        String note


) {
}
