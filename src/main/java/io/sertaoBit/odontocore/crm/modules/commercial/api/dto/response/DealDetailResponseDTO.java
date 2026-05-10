package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response;

import java.util.List;

public record DealDetailResponseDTO(
        DealResponseDTO deal,
        List<DealHistoryResponseDTO> history
) {
}
