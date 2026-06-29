package io.sertaoBit.odontocore.crm.modules.appointment.event;

import io.sertaoBit.odontocore.crm.core.events.DealWonEvent;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import io.sertaoBit.odontocore.crm.modules.appointment.repository.AppointmentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus.AWAITING_SCHEDULE;
import static io.sertaoBit.odontocore.crm.core.enums.AppointmentType.PROCEDURE;

@Component
public class AppointmentEventListener {

    private final AppointmentRepository appointmentRepository;

    public AppointmentEventListener(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @EventListener
    public void onDealWon(DealWonEvent event) {
        List<Appointment> appointments = new ArrayList<>();

        for (DealWonEvent.WonProcedure wp : event.procedures()) {
            for (int i = 1; i <= wp.quantity(); i++) {
                Appointment appointment = Appointment.builder()
                        .type(PROCEDURE)
                        .ticketId(event.ticketId())
                        .dealId(event.dealId())
                        .procedureId(wp.procedureId())
                        .procedureName(wp.name())
                        .customerId(event.customerId())
                        .customerName(event.customerName())
                        .evaluatorId(event.evaluatorId())
                        .assignedTo(event.evaluatorId())
                        .status(AWAITING_SCHEDULE)
                        .sessionIndex(i)
                        .plannedSessions(wp.quantity())
                        .createdBy(event.evaluatorId())
                        .build();
                appointments.add(appointment);

            }
        }
        appointmentRepository.saveAll(appointments);
    }
}
