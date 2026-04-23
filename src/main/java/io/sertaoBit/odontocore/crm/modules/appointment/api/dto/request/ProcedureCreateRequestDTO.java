package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcedureCreateRequestDTO(
        @NotBlank String description,
        @NotNull Double basePrice,
        @NotBlank String category
) {
}
