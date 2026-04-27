package io.sertaoBit.odontocore.crm.modules.crm.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.DealStatus;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DealResponseDTO(
        @NotNull UUID id,
        @NotNull Customer customer,
        @NotNull DealStatus dealStatus,
        @NotBlank List<String> procedures,
        @NotNull BigDecimal negotiationValue,
        @NotNull User closedBy,
        @NotBlank List<String> description,
        @NotNull LocalDateTime closedDate,
        @NotNull LocalDateTime targetDate
) {
}
