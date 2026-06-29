package io.sertaoBit.odontocore.crm.modules.appointment.repository;

import io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    // conflito do batch em UMA query (Trava 1 da ADR-029): SCHEDULED nos pares candidatos
    List<Appointment> findByStatusAndAssignedToInAndScheduledAtIn(
            AppointmentStatus status, Collection<UUID> assignedTos, Collection<LocalDateTime> slots);
}
