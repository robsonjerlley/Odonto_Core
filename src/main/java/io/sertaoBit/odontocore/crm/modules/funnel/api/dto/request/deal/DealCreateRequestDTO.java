package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.deal;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record DealCreateRequestDTO(
        @NotNull Customer customer,
        @NotNull DealStatus dealStatus,
        @NotBlank Set<String> procedures,
        @NotNull BigDecimal negotiationValue,
        @NotNull User closedBy,
        @NotBlank String description,
        @NotNull LocalDate targetDate
) {
}
