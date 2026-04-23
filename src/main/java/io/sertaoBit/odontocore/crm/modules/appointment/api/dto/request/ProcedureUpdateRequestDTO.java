package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProcedureUpdateRequestDTO(
        @NotNull UUID id,
        @NotBlank String description,
        @NotNull Double basePrice,
        @NotBlank String category
) {
}
