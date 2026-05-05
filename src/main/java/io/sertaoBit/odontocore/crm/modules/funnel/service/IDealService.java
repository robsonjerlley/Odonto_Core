package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.deal.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.DealResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public interface IDealService {

    DealResponseDTO create(DealCreateRequestDTO dto);

    DealResponseDTO update(UUID id, DealUpdateRequestDTO dto);

    DealResponseDTO findById(UUID id);

    List<DealResponseDTO> findAll();

    List<DealResponseDTO> findByCustomer(UUID customerId);

    List<DealResponseDTO> findByStatus(DealStatus status);

    List<DealResponseDTO> findClosedByUser(UUID userId);

    List<DealResponseDTO> findByDateRange(LocalDate start, LocalDate end);

    DealResponseDTO updateStatus(UUID id, DealStatus status);

    void delete(UUID id);
}
