package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketChangeStatusRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.service.LeadTicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class LeadTicketController {

    private final LeadTicketService ticketService;

    public LeadTicketController(LeadTicketService ticketService) {

        this.ticketService = ticketService;
    }

    @PostMapping()
    public ResponseEntity<LeadTicketResponseDTO> create(
            @RequestBody @Validated LeadTicketCreateRequestDTO dto
    ) {
        LeadTicketResponseDTO leadTicketResponseDTO = ticketService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(leadTicketResponseDTO);
    }


    @PatchMapping("/{id}/status")
    public ResponseEntity<LeadTicketResponseDTO> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Validated LeadTicketChangeStatusRequestDTO dto
    ) {
        return ResponseEntity.ok(ticketService.changeStatus(id, dto));
    }


    @GetMapping("/{id}")
    public ResponseEntity<LeadTicketResponseDTO> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    @GetMapping()
    public ResponseEntity<Page<LeadTicketResponseDTO>> search(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) UUID assignedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ticketService.search(customerId, status, assignedTo, pageable));
    }

}
