package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycle;

import io.sertaoBit.odontocore.crm.core.enums.Sector;
import jakarta.validation.constraints.Min;

public record RecycleConfigRequestDTO(
        Sector sector,
        @Min(1) int afterDays
) {
}
