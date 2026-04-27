package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.SalesMetricsResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface ISalesMetricsService {

    SalesMetricsResponseDTO findById(UUID id);
    List<SalesMetricsResponseDTO> finAll();
    void deleteById(UUID id);
}
