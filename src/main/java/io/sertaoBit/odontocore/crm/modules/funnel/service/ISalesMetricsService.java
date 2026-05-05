package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.SalesMetricsResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.ContactChannel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public interface ISalesMetricsService {


    List<SalesMetricsResponseDTO> findAll();


    SalesMetricsResponseDTO findById(UUID id);


    void deleteById(UUID id);


    // ====== Query Metrics ======

    SalesMetricsResponseDTO getSummaryToday();

    List<SalesMetricsResponseDTO> getByChannel(ContactChannel channel);


    List<SalesMetricsResponseDTO> getByDepartment(UUID departmentId);

    List<SalesMetricsResponseDTO> getByUser(UUID userId);


    List<SalesMetricsResponseDTO> getByDateRange(LocalDate startDate, LocalDate endDate);

    // ====== Performance Indicators ======

    BigDecimal getSuccessRate();


    BigDecimal getConversionRate();

    // ====== TRENDING & AGGREGATIONS ======

    List<SalesMetricsResponseDTO> getTrendingData(LocalDate startDate, LocalDate endDate);


    Map<String, Object> getMetricsByChannel();


    Map<String, Object> getMetricsByDepartment();


    List<SalesMetricsResponseDTO> getTopPerformingUsers(int limit);

    // ====== Maintenance & Calculation ======

    void calculateAndUpdateMetrics();


    void recalculateMetricsForDate(LocalDate date);

    void clearAllMetrics();
}
