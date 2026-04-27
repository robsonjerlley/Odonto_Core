package io.sertaoBit.odontocore.crm.modules.crm.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactOutcome;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Ticket;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ContactLogResponseDTO(
        @NotNull UUID id,
        @NotNull Customer customer,
        @NotNull Ticket ticket,
        @NotNull User contactBy,
        @NotNull ContactChannel contactChannel,
        @NotBlank List<String> description,
        @NotNull ContactOutcome contactOutcome,
        @NotNull LocalDateTime contactDate,
        @NotNull LocalDate nextFollowUp,
        @NotNull BigDecimal investmentAmount,
        @NotNull BigDecimal conversionValue

) {
}
