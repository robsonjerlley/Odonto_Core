package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConflictWarning(
        UUID assignedTo,
        LocalDateTime slot,
        List<UUID> appointmentIds
) {
}
