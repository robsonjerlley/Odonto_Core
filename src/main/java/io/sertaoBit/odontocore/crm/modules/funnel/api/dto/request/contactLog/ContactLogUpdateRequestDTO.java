package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.ContactChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
@Builder
public record ContactLogUpdateRequestDTO(
        @NotNull UUID id,
        @NotNull UUID customerId,
        @NotNull UUID ticketID,
        @NotNull UUID contactByUserId,
        @NotNull ContactChannel contactChannel,
        @NotBlank String description,
        @NotNull ContactOutcome contactOutcome,
        @NotNull LocalDate nextFollowUp,
        @NotNull BigDecimal investmentAmount,
        @NotNull BigDecimal conversionValue
) {
}
