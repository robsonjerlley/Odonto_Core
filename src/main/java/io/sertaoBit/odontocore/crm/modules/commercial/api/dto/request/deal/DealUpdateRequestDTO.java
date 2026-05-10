package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DealUpdateRequestDTO(

        @NotEmpty List<DealProcedureDTO> procedures


) {
}
