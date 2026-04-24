package io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request;

import io.sertaoBit.odontocore.crm.modules.appointment.domain.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Procedure;
import io.sertaoBit.odontocore.crm.modules.clinic.domain.model.Clinic;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AppointmentUpdateRequestDTO(
        @NotNull AppointmentStatus status,
        @NotNull Clinic clinic,
        @NotNull User user,
        @NotNull Customer customer,
        @NotNull List<Procedure> procedures,
        @NotNull Double totalValue
) {
}
