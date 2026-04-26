package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.deal;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.DealStatus;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DealCreateRequestDTO(
        @NotNull Customer customer,
        @NotNull DealStatus dealStatus,
        @NotBlank String procedures,
        @NotNull Double negotiationValue,
        @NotNull User closedBy,
        @NotBlank String description
) {
}
