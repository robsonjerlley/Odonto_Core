package io.sertaoBit.odontocore.crm.modules.crm.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Department;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SalesMetricsResponseDTO(
        @NotNull UUID id,
        @NotNull LocalDate period,
        @NotNull ContactChannel contactChannel,
        @NotNull Department department,
        @NotNull User userId,
        @NotNull Integer totalContact,
        @NotNull Integer successfulContact,
        @NotNull Integer failedContact,
        @NotNull Integer pendingFollowUp,
        @NotNull BigDecimal successRate,
        @NotNull BigDecimal conversionRate,
        @NotNull LocalDateTime createdAt,
        @NotNull LocalDateTime updateAt
) {
}
