package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.service.ContactLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contact-logs")
public class ContactLogController {

    private final ContactLogService contactLogService;


    public ContactLogController(ContactLogService contactLogService) {

        this.contactLogService = contactLogService;
    }

    @PostMapping()
    public ResponseEntity<ContactLogResponseDTO> create(
            @RequestBody @Validated ContactLogCreateRequestDTO dto
    ) {
        ContactLogResponseDTO contactLogResponseDTO = contactLogService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(contactLogResponseDTO);
    }


    @GetMapping
    public ResponseEntity<List<ContactLogResponseDTO>> findAll() {
        return ResponseEntity.ok(contactLogService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactLogResponseDTO> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(contactLogService.findById(id));
    }

    @GetMapping("/findByTicketId/{id}")
    public ResponseEntity<List<ContactLogResponseDTO>> findByTicketId(
            @PathVariable UUID ticketId
    ) {
        return ResponseEntity.ok(contactLogService.findByTicketId(ticketId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contactLogService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
