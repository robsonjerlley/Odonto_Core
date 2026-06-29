package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssigneeRequestDTO(
        @NotNull UUID assignedTo
) {
}
