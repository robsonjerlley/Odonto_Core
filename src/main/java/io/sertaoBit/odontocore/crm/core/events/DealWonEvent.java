package io.sertaoBit.odontocore.crm.core.events;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record DealWonEvent(
        UUID clinicId,
        UUID dealId,
        UUID ticketId,
        UUID customerId,
        String customerName,
        UUID evaluatorId,
        LocalDateTime closedAt,
        List<WonProcedure> procedures


) {
    public DealWonEvent {
        procedures = List.copyOf(procedures);
    }

    public record WonProcedure(
            UUID procedureId,
            String name,
            String code,
            int quantity
            ) {}

}
