package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduleRequestDTO(
       @NotNull LocalDateTime scheduledAt,
        UUID assignedTo
) {
}
