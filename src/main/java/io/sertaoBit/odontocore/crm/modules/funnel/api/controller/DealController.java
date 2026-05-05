package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.deal.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.service.IDealService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deal")
public class DealController {

    private final IDealService dealService;

    public DealController(IDealService dealService) {
        this.dealService = dealService;
    }

    @PostMapping("/create")
    public ResponseEntity<DealResponseDTO> create(@RequestBody @Validated DealCreateRequestDTO dto) {
        DealResponseDTO responseDTO = dealService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<DealResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody @Validated DealUpdateRequestDTO dto) {

        return ResponseEntity.ok(dealService.update(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<DealResponseDTO>> findAll() {
        return ResponseEntity.ok(dealService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DealResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(dealService.findById(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<DealResponseDTO>> findByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(dealService.findByCustomer(customerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DealResponseDTO>> findByStatus(@PathVariable DealStatus status) {
        return ResponseEntity.ok(dealService.findByStatus(status));
    }

    @GetMapping("/closedByUser/{userId}")
    public ResponseEntity<List<DealResponseDTO>> findClosedByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(dealService.findClosedByUser(userId));
    }

    @GetMapping("/dateRange/{start}/{end}")
    public ResponseEntity<List<DealResponseDTO>> findByDateRange(
            @PathVariable LocalDate startDate, @PathVariable LocalDate endDate) {
        return ResponseEntity.ok(dealService.findByDateRange(startDate, endDate));
    }

    @PatchMapping("/updateStatus/{id}/{status}")
    public ResponseEntity<DealResponseDTO> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Validated DealStatus status) {
        return ResponseEntity.ok(dealService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dealService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
