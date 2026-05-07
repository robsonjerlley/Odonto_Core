package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket;


import io.sertaoBit.odontocore.crm.core.enums.Sector;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadTicketCreateRequestDTO(
        @NotNull UUID customerId,
        @NotNull Sector currentSector,
        UUID assignedTo,
        LocalDateTime scheduledAt

) {
}
