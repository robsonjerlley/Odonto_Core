package io.sertaoBit.odontocore.crm.modules.crm.service.impl;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.SalesMetricsResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.SalesMetrics;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.ISalesMetricsMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.IDepartmentRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ISalesMetricsRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.ISalesMetricsService;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesMetricsServiceImpl implements ISalesMetricsService {

    private final ISalesMetricsRepository salesMetricsRepository;
    private final ICustomerRepository customerRepository;
    private final IUserRepository userRepository;
    private final ISalesMetricsMapper salesMetricsMapper;
    private final IDepartmentRepository departmentRepository;

    public SalesMetricsServiceImpl(
            ISalesMetricsRepository salesMetricsRepository,
            ICustomerRepository customerRepository,
            IUserRepository userRepository,
            ISalesMetricsMapper salesMetricsMapper,
            IDepartmentRepository departmentRepository
    ) {
        this.salesMetricsRepository = salesMetricsRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.salesMetricsMapper = salesMetricsMapper;

        this.departmentRepository = departmentRepository;
    }


    @Override
    @Transactional(readOnly = true)
    public List<SalesMetricsResponseDTO> findAll() {
        return salesMetricsRepository.findAll()
                .stream().map(salesMetricsMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SalesMetricsResponseDTO findById(UUID id) {
        return salesMetricsRepository.findById(id)
                .map(salesMetricsMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("SalesMetrics not found by id: " + id));

    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (!salesMetricsRepository.existsById(id)) {
            throw new RuntimeException("SalesMetrics not found by id: " + id);
        }
        salesMetricsRepository.deleteById(id);
    }

    // ====== Query Metrics ====== //

    @Override
    @Transactional(readOnly = true)
    public SalesMetricsResponseDTO getSummaryToday() {
        LocalDate today = LocalDate.now();

        return salesMetricsRepository.findAll().stream()
                .filter(sm -> sm.getPeriod().equals(today))
                .map(salesMetricsMapper::toResponseDTO)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("SalesMetrics not found today"));

    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesMetricsResponseDTO> getByChannel(ContactChannel channel) {
        if (channel == null) {
            throw new RuntimeException("SalesMetrics not found by channel");
        }

        return salesMetricsRepository.findAll().stream()
                .filter(sm -> sm.getContactChannel() == channel)
                .map(salesMetricsMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesMetricsResponseDTO> getByDepartment(UUID departmentId) {
        if (departmentId == null) {
            throw new RuntimeException("SalesMetrics not found by department");
        }

        if (!departmentRepository.existsById(departmentId)) {
            throw new RuntimeException("SalesMetrics not found department by id: " + departmentId);
        }

        return salesMetricsRepository.findAll().stream()
                .filter(sm -> sm.getDepartment().getId().equals(departmentId))
                .map(salesMetricsMapper::toResponseDTO)
                .collect(Collectors.toList());

    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesMetricsResponseDTO> getByUser(UUID userId) {
        if (userId == null) {
            throw new RuntimeException("userId cannot be null ");
        }
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found by id: " + userId);
        }
        return salesMetricsRepository.findAll().stream()
                .map(salesMetricsMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesMetricsResponseDTO> getByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new RuntimeException("startDate or endDate cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("startDate is after endDate");
        }

        return salesMetricsRepository.findAll().stream()
                .filter(sm -> {
                    LocalDate period = sm.getPeriod();
                    return period.isAfter(startDate) && period.isBefore(endDate);
                })
                .map(salesMetricsMapper::toResponseDTO)
                .collect(Collectors.toList());
    }


    // ====== Performance Indicators ====== //

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getSuccessRate() {
        List<SalesMetrics> allMetrics = salesMetricsRepository.findAll();

        if (allMetrics.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Integer totalSuccessful = allMetrics.stream()
                .mapToInt(SalesMetrics::getSuccessfulContact)
                .sum();

        Integer totalContacts = allMetrics.stream()
                .mapToInt(SalesMetrics::getTotalContact)
                .sum();

        if (totalContacts == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf((double) totalSuccessful / totalContacts * 100)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getConversionRate() {
        List<SalesMetrics> allMetrics = salesMetricsRepository.findAll();

        if (allMetrics.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Integer conversion = allMetrics.stream()
                .mapToInt(SalesMetrics::getSuccessfulContact)
                .sum();

        Integer totalContacts = allMetrics.stream()
                .mapToInt(SalesMetrics::getTotalContact)
                .sum();

        if (totalContacts == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf((double) conversion / totalContacts * 100)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesMetricsResponseDTO> getTrendingData(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("start date cannot be  after end date");
        }

        return salesMetricsRepository.findAll().stream()
                .filter(sm -> {
                    LocalDate period = sm.getPeriod();
                    return !period.isAfter(startDate) && !period.isBefore(endDate);
                })
                .sorted(Comparator.comparing(SalesMetrics::getPeriod))
                .map(salesMetricsMapper::toResponseDTO)
                .collect(Collectors.toList());

    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMetricsByChannel() {
        Map<ContactChannel, List<SalesMetrics>> groupByChannel =
                salesMetricsRepository.findAll().stream()
                        .collect(Collectors.groupingBy(SalesMetrics::getContactChannel));

        Map<String, Object> result = new HashMap<>();

        groupByChannel.forEach((channel, metrics) -> {
            Integer totalContacts = metrics.stream()
                    .mapToInt(SalesMetrics::getTotalContact)
                    .sum();

            Integer successful = metrics.stream()
                    .mapToInt(SalesMetrics::getSuccessfulContact)
                    .sum();

            BigDecimal rate = totalContacts > 0
                    ? BigDecimal.valueOf((double) successful / totalContacts * 100)
                    : BigDecimal.ZERO;

            Map<String, Object> channelData = Map.of(
                    "channel", channel.toString(),
                    "totalContact", totalContacts,
                    "sucessful", successful,
                    "sucessfulRate", rate
            );

            result.put("channelData", channelData);
        });
        return result;
    }

    @Override
    public Map<String, Object> getMetricsByDepartment() {
        return Map.of();
    }

    @Override
    public List<SalesMetricsResponseDTO> getTopPerformingUsers(int limit) {
        return List.of();
    }

    @Override
    public void calculateAndUpdateMetrics() {

    }

    @Override
    public void recalculateMetricsForDate(LocalDate date) {

    }

    @Override
    public void clearAllMetrics() {

    }
}
