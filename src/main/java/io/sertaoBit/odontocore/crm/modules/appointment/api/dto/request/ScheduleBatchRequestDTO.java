package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScheduleBatchRequestDTO(
        @Valid @NotEmpty List<BatchItem> items
) {
}
