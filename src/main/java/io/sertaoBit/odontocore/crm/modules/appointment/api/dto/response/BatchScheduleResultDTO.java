package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response;

import java.util.List;

public record BatchScheduleResultDTO(
        List<AppointmentResponseDTO> scheduled,
        List<ConflictWarning> warnings
) {
}
