package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.ticket.TicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.ticket.TicketUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.TicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.service.ITicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final ITicketService ticketService;

    public TicketController(ITicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/create")
    public ResponseEntity<TicketResponseDTO> create(@RequestBody @Validated TicketCreateRequestDTO dto) {
        TicketResponseDTO ticketResponseDTO = ticketService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketResponseDTO);
    }

    @PatchMapping("/update/{id}/{dto}")
    public ResponseEntity<TicketResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody @Validated TicketUpdateRequestDTO dto) {
        return ResponseEntity.ok(ticketService.update(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> findAll() {
        return ResponseEntity.ok(ticketService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> findById(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(ticketService.findById(ticketId));
    }

    @GetMapping("/findByCustomer/{customerId}")
    public ResponseEntity<List<TicketResponseDTO>> findByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ticketService.findByCustomer(customerId));
    }

    @GetMapping("/ticketStatus/{ticketStatus}")
    public ResponseEntity<List<TicketResponseDTO>> findByTicketStatus(@PathVariable TicketStatus ticketStatus) {
        return ResponseEntity.ok(ticketService.findByTicketStatus(ticketStatus));
    }

    @GetMapping("assignedToUser/{userId}")
    public ResponseEntity<List<TicketResponseDTO>> findByAssignedToUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ticketService.findByAssignedToUser(userId));
    }

    @PatchMapping("/updateStatus/{id}/{ticketStatus}")
    public ResponseEntity<TicketResponseDTO> updateStatus(
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
