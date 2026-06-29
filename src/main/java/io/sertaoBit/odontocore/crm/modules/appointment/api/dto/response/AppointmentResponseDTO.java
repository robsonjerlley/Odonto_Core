package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response;

import io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.core.enums.AppointmentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponseDTO(
        UUID id,
        UUID clinicId,
        UUID dealId,
        UUID procedureId,
        String procedureName,
        AppointmentType type,
        UUID customerId,
        String customerName,
        UUID evaluatorId,
        UUID assignedTo,
        AppointmentStatus status,
        LocalDateTime scheduledAt,
        Integer sessionIndex,
        Integer plannedSessions,
        String note,
        String cancelReason

) {
}
