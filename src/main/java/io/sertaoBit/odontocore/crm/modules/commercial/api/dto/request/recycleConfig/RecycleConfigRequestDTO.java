package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig;

import jakarta.validation.constraints.Min;

public record RecycleConfigRequestDTO(
        @Min(1) int afterDays

        ) {
}
