package io.sertaoBit.odontocore.crm.modules.commercial.api.controller;


import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.ApplyDiscountRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.CloseDealRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.DealDetailResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.mapper.DealMapper;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.service.DealService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deals")
public class DealController {

    private final DealService dealService;
    private final DealMapper dealMapper;

    public DealController(
            DealService dealService,
            DealMapper dealMapper
    ) {
        this.dealService = dealService;
        this.dealMapper = dealMapper;
    }


    @PostMapping("/{ticketId}")
    public ResponseEntity<DealResponseDTO> create(
            @PathVariable UUID ticketId,
            @RequestBody @Validated DealCreateRequestDTO dto
    ) {

        Deal deal = dealService.create(ticketId, dto);

        return ResponseEntity.status(
                        HttpStatus.CREATED)
                .body(dealMapper.toResponseDTO(deal)
                );

    }

    @PatchMapping("/{id}")
    public ResponseEntity<DealResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody @Validated DealUpdateRequestDTO dto
    ) {

        Deal deal = dealService.update(id, dto);
        return ResponseEntity.ok(dealMapper.toResponseDTO(deal));

    }

    @GetMapping("/ticketId/{ticketId}")
    public ResponseEntity<DealResponseDTO> findByTicket(@PathVariable UUID ticketId) {
        return dealService.findByTicket(ticketId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/discount")
    public ResponseEntity<DealResponseDTO> applyDiscount(
            @PathVariable UUID id,
            @RequestBody @Validated ApplyDiscountRequestDTO dto
    ) {
        Deal deal = dealService.applyDiscount(id, dto);
        return ResponseEntity.ok(dealMapper.toResponseDTO(deal));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DealResponseDTO> closeDeal(
            @PathVariable UUID id,
            @RequestBody @Validated CloseDealRequestDTO dto
    ) {
        Deal deal = dealService.closeDeal(id, dto.paymentMethod());
        return ResponseEntity.ok(dealMapper.toResponseDTO(deal));
    }

    @GetMapping("/{id}/dealHistory")
    public ResponseEntity<DealDetailResponseDTO> getDealWithHistory(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(dealService.getDealWithHistory(id));
    }

}

