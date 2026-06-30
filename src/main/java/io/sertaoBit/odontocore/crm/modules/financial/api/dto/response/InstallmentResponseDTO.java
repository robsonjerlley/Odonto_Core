package io.sertaoBit.odontocore.crm.modules.financial.api.dto.response;

import io.sertaoBit.odontocore.crm.core.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentResponseDTO(
        UUID id,
        UUID dealId,
        UUID customerId,
        String customerName,
        Integer sequence,
        Integer totalInstallments,
        LocalDate dueDate,
        BigDecimal expectedAmount,
        PaymentStatus status,
        boolean overdue,
        BigDecimal paidAmount,
        LocalDate paidAt

) {
}
