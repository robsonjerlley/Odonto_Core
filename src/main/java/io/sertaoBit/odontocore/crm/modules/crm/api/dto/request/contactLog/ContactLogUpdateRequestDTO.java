package io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactOutCome;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Ticket;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContactLogUpdateRequestDTO(
        @NotNull UUID id,
        @NotNull Customer customer,
        @NotNull Ticket ticket,
        @NotNull User contactBy,
        @NotNull ContactChannel contactChannel,
        @NotBlank String description,
        @NotNull ContactOutCome contactOutcome,
        @NotNull LocalDate nextFollowUp,
        @NotNull BigDecimal investmentAmount,
        @NotNull BigDecimal conversionValue
) {
}
