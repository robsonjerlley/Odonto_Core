package io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;


public record ProcedureUpdateRequestDTO(
        @NotNull String name,
        String code,
        @NotNull BigDecimal defaultPrice,
        Integer estimatedDuration,
        boolean active
) {
}
