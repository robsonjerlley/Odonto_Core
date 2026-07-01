package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.deal;

import io.sertaoBit.odontocore.crm.core.enums.PaymentMethod;
import io.sertaoBit.odontocore.crm.core.enums.Sector;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DealResponseDTO(
        UUID id,
        UUID ticketId,
        UUID createdBy,
        Sector createdBySector,
        List<DealProcedureResponseDTO> items,
        BigDecimal totalValue,
        BigDecimal discountPct,
        UUID discountApprovedBy,
        BigDecimal finalValue,
        PaymentMethod paymentMethod,
        UUID closedBy,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        LocalDateTime updatedAt



) {
}
