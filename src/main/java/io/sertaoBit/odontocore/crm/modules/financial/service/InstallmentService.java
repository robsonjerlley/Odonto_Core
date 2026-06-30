package io.sertaoBit.odontocore.crm.modules.financial.service;

import io.sertaoBit.odontocore.crm.modules.financial.api.dto.request.PayRequestDTO;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.InstallmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InstallmentService {

    InstallmentResponseDTO pay(UUID installmentId, PayRequestDTO dto);

    Page<InstallmentResponseDTO> getInstallments(List<UUID> dealIds, Pageable pageable);

    Page<InstallmentResponseDTO> getInstallmentsByCustomerId(UUID customerId, List<UUID> installmentIds, Pageable pageable);

    Page<InstallmentResponseDTO> cashflow(LocalDate from, LocalDate to, Pageable pageable);

    void unpay(UUID installmentId);
}
