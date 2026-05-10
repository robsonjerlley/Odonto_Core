package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal;

import jakarta.validation.constraints.NotBlank;

public record CloseDealRequestDTO(

        @NotBlank String paymentMethod
) {
}
