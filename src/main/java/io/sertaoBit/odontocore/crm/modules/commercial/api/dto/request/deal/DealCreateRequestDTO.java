package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal;

import io.sertaoBit.odontocore.crm.shared.DealProcedureDTO;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DealCreateRequestDTO(

        @NotEmpty List<DealProcedureDTO> procedures

) {
}
