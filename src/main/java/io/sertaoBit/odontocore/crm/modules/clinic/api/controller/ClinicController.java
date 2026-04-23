package io.sertaoBit.odontocore.crm.modules.clinic.api.controller;

import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.response.ClinicResponseDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.service.IClinicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinic")
public class ClinicController {

    private final IClinicService clinicService;

    public ClinicController(IClinicService clinicService) {
        this.clinicService = clinicService;
    }

    @PostMapping("/create")
    public ResponseEntity<ClinicResponseDTO> create(
            @RequestBody @Valid ClinicCreateRequestDTO createRequestDTO) {
        ClinicResponseDTO responseDTO = clinicService.create(createRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);

    }

    @PatchMapping("/update/{cnpj}")
    public ResponseEntity<ClinicResponseDTO> update(
            @PathVariable String cnpj, @RequestBody @Valid ClinicUpdateRequestDTO updateRequestDTO) {
        return ResponseEntity.ok().body(clinicService.update(cnpj, updateRequestDTO));
    }

    @GetMapping
    public ResponseEntity<List<ClinicResponseDTO>> findAll() {
        return ResponseEntity.ok().body(clinicService.findAll());
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<ClinicResponseDTO> findByCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(clinicService.findByCnpj(cnpj));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(clinicService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        clinicService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
