package io.sertaoBit.odontocore.crm.modules.catalog.api.controller;

import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.service.ProcedureService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/procedures")
public class ProcedureController {

    private final ProcedureService procedureService;

    public ProcedureController(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }


    @PostMapping
    public ResponseEntity<ProcedureResponseDTO> create(@RequestBody @Validated ProcedureCreateRequestDTO dto) {

        return ResponseEntity.status(CREATED).body(procedureService.create(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProcedureResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody @Validated ProcedureUpdateRequestDTO dto) {

        return ResponseEntity.ok(procedureService.update(id, dto));
    }

    @GetMapping
    public ResponseEntity<Page<ProcedureResponseDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(procedureService.search(name, code, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {

        procedureService.softDelete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
