package io.sertaoBit.odontocore.crm.modules.appointment.api.controller;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.ProcedureUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.service.IProcedureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/procedure")
public class ProcedureController {

    private final IProcedureService procedureService;

    public ProcedureController(IProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    @PostMapping("/create")
    public ResponseEntity<ProcedureResponseDTO> create(
            @RequestBody @Valid ProcedureCreateRequestDTO dto) {
        ProcedureResponseDTO responseDTO = procedureService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<ProcedureResponseDTO> update(
            @PathVariable UUID id, @RequestBody @Valid ProcedureUpdateRequestDTO dto) {
        return ResponseEntity.ok().body(procedureService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedureResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(procedureService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProcedureResponseDTO>> findAll() {
        return ResponseEntity.ok().body(procedureService.findAll());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<List<ProcedureResponseDTO>> findByName(@PathVariable String name) {
        return ResponseEntity.ok().body(procedureService.findByName(name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        procedureService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
