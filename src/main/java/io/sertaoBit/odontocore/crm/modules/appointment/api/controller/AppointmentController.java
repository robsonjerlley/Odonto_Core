package io.sertaoBit.odontocore.crm.modules.appointment.api.controller;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.AppointmentCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.AppointmentUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.AppointmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.service.IAppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointment")
public class AppointmentController {

    private final IAppointmentService appointmentService;

    public AppointmentController(IAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/create")
    public ResponseEntity<AppointmentResponseDTO> create(
            @RequestBody @Valid AppointmentCreateRequestDTO dot) {
        AppointmentResponseDTO responseDTO = appointmentService.create(dot);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<AppointmentResponseDTO> update(
            @PathVariable UUID id, @RequestBody @Valid AppointmentUpdateRequestDTO dot) {
        return ResponseEntity.ok().body(appointmentService.update(id, dot));
    }


    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(appointmentService.findById(id));
    }


    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> findAll() {
        return ResponseEntity.ok().body(appointmentService.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        appointmentService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
