package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketChangeStatusRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;

import io.sertaoBit.odontocore.crm.modules.funnel.service.LeadTicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        return ResponseEntity.ok(ticketService.changeStatus(id, dto.status()));
    }


    @GetMapping
    public ResponseEntity<List<LeadTicketResponseDTO>> findAll() {

        return ResponseEntity.ok(ticketService.findAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<LeadTicketResponseDTO> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ticketService.findById(id));
    }


    @GetMapping("/findByCustomer/{customerId}")
    public ResponseEntity<List<LeadTicketResponseDTO>> findByCustomer(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(ticketService.findByCustomer(customerId));
    }


    @GetMapping("/ticketStatus/{status}")
    public ResponseEntity<List<LeadTicketResponseDTO>> findByTicketStatus(
            @PathVariable TicketStatus status
    ) {
        return ResponseEntity.ok(ticketService.findByStatus(status));
    }


    @GetMapping("/assignedToUser/{userId}")
    public ResponseEntity<List<LeadTicketResponseDTO>> findByAssignedToUser(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(ticketService.findByAssignedToUser(userId));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        ticketService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
