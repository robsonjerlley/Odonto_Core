package io.sertaoBit.odontocore.crm.modules.appointment.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.*;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.AppointmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.service.AppointmentService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;
import static org.springframework.http.HttpStatusCode.valueOf;

@RestController
@RequestMapping("api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }


    @PatchMapping("/{id}/schedule")
    public ResponseEntity<AppointmentResponseDTO> schedule(
            @PathVariable UUID id,
            @RequestBody @Validated ScheduleRequestDTO dto
    ) {
        return ResponseEntity.ok(appointmentService.schedule(id, dto));
    }


    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponseDTO> reschedule(
            @PathVariable UUID id,
            @RequestBody @Validated RescheduleRequestDTO dto
    ) {
        return ResponseEntity.ok(appointmentService.reschedule(id, dto));
    }


    @PatchMapping("/{id}/assignee")
    public ResponseEntity<AppointmentResponseDTO> assignee(
            @PathVariable UUID id,
            @RequestBody @Validated AssigneeRequestDTO dto
    ) {
        return ResponseEntity.ok(appointmentService.assignee(id, dto));
    }


    @PatchMapping("/schedule-batch")
    public ResponseEntity<io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.BatchScheduleResultDTO> scheduleBatch(
            @RequestBody List<BatchItem> dto
    ) {
        return ResponseEntity.ok(appointmentService.scheduleBatch(dto));
    }


    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable UUID id
    ) {
        return ResponseEntity.status(valueOf(200)).build();
    }


    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancel(
            @PathVariable UUID id,
            @RequestBody @Validated CancelRequestDTO dto
    ) {
        return ResponseEntity.ok(appointmentService.cancel(id, dto));
    }


    @GetMapping(params = "status")
    public ResponseEntity<Page<AppointmentResponseDTO>> getStatus(
            @RequestParam(required = false) AppointmentStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(appointmentService.getStatus(status, pageable));
    }

    @GetMapping(params = {"assignedTo","from", "to"})
    public ResponseEntity<Page<AppointmentResponseDTO>> getAssignedBetween(
            @RequestParam UUID assignedTo,
            @RequestParam @DateTimeFormat(iso = DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(appointmentService.getAssignedBetween(assignedTo, from, to, pageable));
    }

}
