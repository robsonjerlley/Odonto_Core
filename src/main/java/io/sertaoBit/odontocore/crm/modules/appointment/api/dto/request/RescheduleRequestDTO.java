package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;


public record RescheduleRequestDTO(
        @NotNull LocalDateTime scheduledAt
) {
}
