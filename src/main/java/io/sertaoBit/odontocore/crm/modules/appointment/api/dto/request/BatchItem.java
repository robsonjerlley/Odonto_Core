package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record BatchItem(
        @NotNull UUID appointmentId,
        @NotNull LocalDateTime scheduledAt,
        UUID assignedTo
) {
}
