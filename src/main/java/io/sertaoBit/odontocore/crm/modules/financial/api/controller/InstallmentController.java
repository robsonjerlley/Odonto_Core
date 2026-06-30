package io.sertaoBit.odontocore.crm.modules.financial.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.PaymentStatus;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.request.PayRequestDTO;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.CashflowMonthDTO;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.InstallmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.financial.service.InstallmentService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/installments")
public class InstallmentController {

    private final InstallmentService installmentService;

    public InstallmentController(InstallmentService installmentService) {
        this.installmentService = installmentService;
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<InstallmentResponseDTO> pay(
            @PathVariable UUID id,
            @RequestBody @Validated PayRequestDTO dto
    ) {
        return ResponseEntity.ok(installmentService.pay(id, dto));
    }

    @PatchMapping("/{id}/unpay")
    public ResponseEntity<Void> unpay(
            @PathVariable UUID id
    ) {
        installmentService.unpay(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping(params = "month")
    public ResponseEntity<Page<InstallmentResponseDTO>> getInstallments(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) PaymentStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable page
    ) {
        return ResponseEntity.ok(installmentService.getInstallments(month, status, page));
    }

    @GetMapping(params = "customerId")
    public ResponseEntity<Page<InstallmentResponseDTO>> getInstallmentsByCustomerId(
            @RequestParam UUID customerId,
            @ParameterObject @PageableDefault(size = 20) Pageable page
    ) {
        return ResponseEntity.ok(installmentService.getInstallmentsByCustomerId(customerId, page));
    }

    @GetMapping("/cashflow")
    public ResponseEntity<List<CashflowMonthDTO>> cashflow(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to
    ) {
        return ResponseEntity.ok(installmentService.cashflow(from, to));
    }

}
