package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record DealResponseDTO(
        @NotNull UUID id,
        @NotNull Customer customer,
        @NotNull DealStatus dealStatus,
        @NotBlank Set<String> procedures,
        @NotNull BigDecimal negotiationValue,
        @NotNull User closedBy,
        @NotBlank String description,
        @NotNull LocalDateTime closedDate,
        @NotNull LocalDate targetDate
) {
}
