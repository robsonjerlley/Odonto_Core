package io.sertaoBit.odontocore.crm.modules.appointment.repository;

import io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByStatusAndAssignedToInAndScheduledAtIn(
            AppointmentStatus status, Collection<UUID> assignedTos, Collection<LocalDateTime> slots);
}
