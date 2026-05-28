package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.service.ContactLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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


    @GetMapping("/{id}")
    public ResponseEntity<ContactLogResponseDTO> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(contactLogService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ContactLogResponseDTO>> search(
            @RequestParam(required = false) UUID ticketId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(contactLogService.search(ticketId, pageable));
    }

}
