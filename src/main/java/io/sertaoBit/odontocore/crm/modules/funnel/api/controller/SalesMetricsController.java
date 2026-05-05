package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.SalesMetricsResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.funnel.service.ISalesMetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metrics")
public class SalesMetricsController {

    private final ISalesMetricsService salesMetricsService;


    public SalesMetricsController(ISalesMetricsService salesMetricsService) {
        this.salesMetricsService = salesMetricsService;
    }

    @GetMapping()
    public ResponseEntity<List<SalesMetricsResponseDTO>> findAll() {
        return ResponseEntity.ok(salesMetricsService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesMetricsResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(salesMetricsService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        salesMetricsService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ====== Query Metrics ======

    @PutMapping("/summary/today")
    public ResponseEntity<SalesMetricsResponseDTO> getSummaryToday() {
        return ResponseEntity.ok(salesMetricsService.getSummaryToday());
    }

    @PostMapping("/channel/{channel}")
    public ResponseEntity<List<SalesMetricsResponseDTO>> getByChannel(@PathVariable ContactChannel channel) {
        return ResponseEntity.ok(salesMetricsService.getByChannel(channel));
    }


    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<SalesMetricsResponseDTO>> getByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(salesMetricsService.getByDepartment(departmentId));
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SalesMetricsResponseDTO>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(salesMetricsService.getByUser(userId));
    }


    @GetMapping("/dateRange/{startDate}/{endDate}")
    public ResponseEntity<List<SalesMetricsResponseDTO>> getByDateRange(
            @PathVariable LocalDate startDate,
            @PathVariable LocalDate endDate) {
        return ResponseEntity.ok(salesMetricsService.getByDateRange(startDate, endDate));
    }

    // ====== PERFORMANCE INDICATORS ======


    @GetMapping("/indicators/successRate")
    public ResponseEntity<BigDecimal> getSuccessRate() {
        return ResponseEntity.ok(salesMetricsService.getSuccessRate());
    }

    @GetMapping("/indicators/conversionRate")
    public ResponseEntity<BigDecimal> getConversionRate() {
        return ResponseEntity.ok(salesMetricsService.getConversionRate());
    }

    // ====== TRENDING & AGGREGATIONS ======


    @GetMapping("/trending/{startDate}/{endDate}")
    public ResponseEntity<List<SalesMetricsResponseDTO>> getTrendingData(
            @PathVariable LocalDate startDate,
            @PathVariable LocalDate endDate) {
        return ResponseEntity.ok(salesMetricsService.getTrendingData(startDate, endDate));
    }

    @GetMapping("/grouped/channels")
    public ResponseEntity<Map<String, Object>> getMetricsByChannel() {
        return ResponseEntity.ok(salesMetricsService.getMetricsByChannel());
    }

    @GetMapping("/grouped/departments")
    public ResponseEntity<Map<String, Object>> getMetricsByDepartment() {
        return ResponseEntity.ok(salesMetricsService.getMetricsByDepartment());
    }

    @GetMapping("/topPerformers/{limit}")
    public ResponseEntity<List<SalesMetricsResponseDTO>> getTopPerformingUsers(@PathVariable int limit) {
        return ResponseEntity.ok(salesMetricsService.getTopPerformingUsers(limit));
    }

    // ====== Maintenance & Calculation ======

    @GetMapping("/updateMetrics")
    public ResponseEntity<Void>calculateAndUpdateMetrics(){
       salesMetricsService.calculateAndUpdateMetrics();
       return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deletMetrics/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        salesMetricsService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllMetrics() {
        salesMetricsService.clearAllMetrics();
        return ResponseEntity.noContent().build();
    }

}
