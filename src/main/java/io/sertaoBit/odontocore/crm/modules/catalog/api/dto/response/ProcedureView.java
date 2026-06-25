package io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcedureView (

        UUID id,
        String name,
        String code,
        BigDecimal defaultPrice,
        Integer estimatedDuration

) {
}
