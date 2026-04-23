package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProcedureResponseDTO(
        @NotNull UUID id,
        @NotBlank String description,
        @NotNull Double basePrice,
        @NotBlank String category
) {
}
