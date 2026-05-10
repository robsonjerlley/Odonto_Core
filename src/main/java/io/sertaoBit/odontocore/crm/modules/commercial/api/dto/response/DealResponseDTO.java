package io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response;

import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealProcedureDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DealResponseDTO(
        UUID id,
        UUID ticketId,
        UUID createdBy,
        Sector createdBySector,
        List<DealProcedureDTO> procedures,
        BigDecimal totalValue,
        BigDecimal discountPct,
        UUID discountApprovedBy,
        BigDecimal finalValue,
        String paymentMethod,
        UUID closedBy,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        LocalDateTime updatedAt



) {
}
