package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactOutcome;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Ticket;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record ContactLogCreateRequestDTO(
        @NotNull Customer customer,
        @NotNull Ticket ticket,
        @NotNull User contactBy,
        @NotNull ContactChannel contactChannel,
        @NotBlank String description,
        @NotNull ContactOutcome contactOutcome,
        @NotNull LocalDate nextFollowUp,
        @NotNull BigDecimal investmentAmount,
        @NotNull BigDecimal conversionValue


) {
}
