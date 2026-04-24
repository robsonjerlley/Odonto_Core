package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response;

import io.sertaoBit.odontocore.crm.modules.appointment.domain.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Procedure;
import io.sertaoBit.odontocore.crm.modules.clinic.domain.model.Clinic;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentResponseDTO(
        @NotNull UUID id,
        @NotNull AppointmentStatus status,
        @NotNull LocalDateTime createdAt,
        @NotNull Clinic clinic,
        @NotNull User user,
        @NotNull Customer customer,
        @NotNull List<Procedure> procedures,
        @NotNull Double totalValue
) {
}
