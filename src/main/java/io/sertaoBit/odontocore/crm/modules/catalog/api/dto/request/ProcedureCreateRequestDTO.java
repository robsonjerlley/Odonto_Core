package io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProcedureCreateRequestDTO(
        @NotNull String name,
        String code,
        @NotNull BigDecimal defaultPrice,
        int estimatedDuration,
        boolean active

) {
}
