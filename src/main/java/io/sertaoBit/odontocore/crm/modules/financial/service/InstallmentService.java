package io.sertaoBit.odontocore.crm.modules.financial.service;

import io.sertaoBit.odontocore.crm.core.enums.PaymentStatus;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.request.PayRequestDTO;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.CashflowMonthDTO;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.InstallmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface InstallmentService {

    InstallmentResponseDTO pay(UUID installmentId, PayRequestDTO dto);

    Page<InstallmentResponseDTO> getInstallments(YearMonth date, PaymentStatus status, Pageable pageable);

    Page<InstallmentResponseDTO> getInstallmentsByCustomerId(UUID customerId, Pageable pageable);

    List<CashflowMonthDTO> cashflow(YearMonth from, YearMonth to);

    void unpay(UUID installmentId);
}
