package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ScheduleDTO(
        @NotNull LocalDateTime occurredAt,
        @NotBlank String description
) {
}
