package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelRequestDTO(
        @NotBlank String cancelReason
) {
}
