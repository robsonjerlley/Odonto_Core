package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.funnel.service.IContactLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contactsLogs")
public class ContactLogController {

    private final IContactLogService contactLogService;


    public ContactLogController(IContactLogService contactLogService) {
        this.contactLogService = contactLogService;
    }

    @PostMapping("/create")
    public ResponseEntity<ContactLogResponseDTO> create(
            @RequestBody @Validated ContactLogCreateRequestDTO dto) {
        ContactLogResponseDTO contactLogResponseDTO = contactLogService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(contactLogResponseDTO);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<ContactLogResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody @Validated ContactLogUpdateRequestDTO dto) {
        return ResponseEntity.ok(contactLogService.update(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<ContactLogResponseDTO>> findAll() {
        return ResponseEntity.ok(contactLogService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactLogResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(contactLogService.findById(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ContactLogResponseDTO>> findByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(contactLogService.findByCustomer(customerId));
    }

    @GetMapping("/contactByUser/{userId}")
    public ResponseEntity<List<ContactLogResponseDTO>> findByContactByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(contactLogService.findByContactByUser(userId));
    }

    @GetMapping("/contactChannel/{channel}")
    public ResponseEntity<List<ContactLogResponseDTO>>findByChannel(@PathVariable ContactChannel channel) {
        return ResponseEntity.ok(contactLogService.findByChannel(channel));
    }

    @GetMapping("/outcomes/{contactOutcome}")
    public ResponseEntity<List<ContactLogResponseDTO>> findOutcome(@PathVariable ContactOutcome contactOutcome) {
        return ResponseEntity.ok(contactLogService.findOutcome(contactOutcome));
    }

    @GetMapping("/dateRange/{startDate}/{endDate}")
    public ResponseEntity<List<ContactLogResponseDTO>> findByDateRange(
            @PathVariable LocalDate startDate, @PathVariable LocalDate endDate) {
        return ResponseEntity.ok(contactLogService.findByDateRange(startDate, endDate));
    }

    @GetMapping("/pendingFollowUps")
    public ResponseEntity<List<ContactLogResponseDTO>> findWithPendingFollowUp() {
        return ResponseEntity.ok(contactLogService.findWithPendingFollowUp());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contactLogService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
