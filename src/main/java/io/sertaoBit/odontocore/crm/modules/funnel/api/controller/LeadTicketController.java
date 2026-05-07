package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
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

    @PostMapping("/create")
    public ResponseEntity<LeadTicketResponseDTO> create(@RequestBody @Validated LeadTicketCreateRequestDTO dto) {
        LeadTicketResponseDTO leadTicketResponseDTO = ticketService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(leadTicketResponseDTO);
    }

    @PatchMapping("/update/{id}/{dto}")
    public ResponseEntity<LeadTicketResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody @Validated LeadTicketUpdateRequestDTO dto) {
        return ResponseEntity.ok(ticketService.update(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<LeadTicketResponseDTO>> findAll() {
        return ResponseEntity.ok(ticketService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadTicketResponseDTO> findById(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(ticketService.findById(ticketId));
    }

    @GetMapping("/findByCustomer/{customerId}")
    public ResponseEntity<List<LeadTicketResponseDTO>> findByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ticketService.findByCustomer(customerId));
    }

    @GetMapping("/ticketStatus/{ticketStatus}")
    public ResponseEntity<List<LeadTicketResponseDTO>> findByTicketStatus(@PathVariable TicketStatus ticketStatus) {
        return ResponseEntity.ok(ticketService.findByTicketStatus(ticketStatus));
    }

    @GetMapping("assignedToUser/{userId}")
    public ResponseEntity<List<LeadTicketResponseDTO>> findByAssignedToUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ticketService.findByAssignedToUser(userId));
    }

    @PatchMapping("/updateStatus/{id}/{ticketStatus}")
    public ResponseEntity<LeadTicketResponseDTO> updateStatus(
            @PathVariable UUID Id,
            @RequestBody @Validated TicketStatus ticketStatus) {
        return ResponseEntity.ok(ticketService.updateStatus(Id, ticketStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ticketService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
