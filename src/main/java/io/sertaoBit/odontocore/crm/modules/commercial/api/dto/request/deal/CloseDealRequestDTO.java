package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CloseDealRequestDTO(

        @NotBlank String paymentMethod
) {
}
