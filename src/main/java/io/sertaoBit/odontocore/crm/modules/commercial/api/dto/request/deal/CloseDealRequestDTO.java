package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal;

import io.sertaoBit.odontocore.crm.core.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CloseDealRequestDTO(

        @NotNull PaymentMethod paymentMethod,
        @Min(value = 1) Integer installmentCount
) {
}
