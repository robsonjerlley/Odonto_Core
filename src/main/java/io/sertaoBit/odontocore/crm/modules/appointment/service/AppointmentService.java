package io.sertaoBit.odontocore.crm.modules.appointment.service;

import io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.*;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.AppointmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.BatchScheduleResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    AppointmentResponseDTO schedule(UUID id, ScheduleRequestDTO dto);

    AppointmentResponseDTO reschedule(UUID id, RescheduleRequestDTO dto);

    AppointmentResponseDTO assignee(UUID id, AssigneeRequestDTO dto);

    AppointmentResponseDTO cancel(UUID id, CancelRequestDTO dto);

    BatchScheduleResultDTO scheduleBatch(List<BatchItem> items);

    Page<AppointmentResponseDTO> getStatus(AppointmentStatus status, Pageable pageable);

    Page<AppointmentResponseDTO> getAssignedBetween(UUID assignedI, LocalDateTime from, LocalDateTime to, Pageable pageable);

    void complete(UUID id);


}
