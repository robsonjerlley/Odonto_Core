package io.sertaoBit.odontocore.crm.modules.crm.service.impl;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.SalesMetricsResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Department;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.SalesMetrics;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.ISalesMetricsMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.IContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.IDepartmentRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ISalesMetricsRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.ISalesMetricsService;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesMetricsServiceImpl implements ISalesMetricsService {

    private final ISalesMetricsRepository salesMetricsRepository;
    private final IUserRepository userRepository;
    private final ISalesMetricsMapper salesMetricsMapper;
    private final IDepartmentRepository departmentRepository;
    private final IContactLogRepository contactLogRepository;

    public SalesMetricsServiceImpl(
            ISalesMetricsRepository salesMetricsRepository,
            IUserRepository userRepository,
            ISalesMetricsMapper salesMetricsMapper,
            IDepartmentRepository departmentRepository,
            IContactLogRepository contactLogRepository
    ) {
        this.salesMetricsRepository = salesMetricsRepository;
        this.userRepository = userRepository;
        this.salesMetricsMapper = salesMetricsMapper;

        this.departmentRepository = departmentRepository;
        this.contactLogRepository = contactLogRepository;
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
                .filter(sm -> sm.getUserId().equals(userId))
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

                    return !period.isBefore(startDate) && !period.isAfter(endDate);
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
        if (startDate == null || endDate == null) {
            throw new RuntimeException("startDate or endDate cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("start date cannot be  after end date");
        }

        return salesMetricsRepository.findAll().stream()
                .filter(sm -> {
                    LocalDate period = sm.getPeriod();

                    return !period.isBefore(startDate) && !period.isAfter(endDate);
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
                    "successful", successful,
                    "successfulRate", rate
            );


            result.put(channel.toString(), channelData);
        });
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMetricsByDepartment() {
        Map<Department, List<SalesMetrics>> groupByDepartment =
                salesMetricsRepository.findAll().stream()
                        .collect(Collectors.groupingBy(SalesMetrics::getDepartment));

        Map<String, Object> result = new HashMap<>();
        groupByDepartment.forEach((department, metrics) -> {

            Integer departmentTotal = metrics.stream()
                    .mapToInt(SalesMetrics::getTotalContact)
                    .sum();

            Integer successful = metrics.stream()
                    .mapToInt(SalesMetrics::getSuccessfulContact)
                    .sum();

            BigDecimal rate = departmentTotal > 0
                    ? BigDecimal.valueOf((double) successful / departmentTotal * 100)
                    : BigDecimal.ZERO;

            Map<String, Object> departmentData = Map.of(
                    "departmentId", department != null ? department.getId().toString() : "UNKNOWN",
                    "totalContact", departmentTotal,
                    "successful", successful,
                    "successRate", rate
            );


            String key = department != null ? department.getId().toString() : "UNKNOWN";
            result.put(key, departmentData);
        });

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesMetricsResponseDTO> getTopPerformingUsers(int limit) {

        return salesMetricsRepository.findAll().stream()
                .sorted((m1, m2) -> m2.getSuccessRate().compareTo(m1.getSuccessRate()))
                .limit(limit)
                .map(salesMetricsMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void calculateAndUpdateMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        recalculateMetricsForDate(yesterday);
    }

    @Override
    @Transactional
    public void recalculateMetricsForDate(LocalDate date) {
        if (date == null) {
            throw new RuntimeException("Date cannot be null");
        }


        List<ContactLog> contactLogs = contactLogRepository.findByContactDate(date);


        Map<String, List<ContactLog>> grouped = contactLogs.stream()
                .collect(Collectors.groupingBy(cl ->
                        String.format("%s_%s_%s",
                                cl.getContactChannel(),
                                cl.getCustomer().getDepartment().getId(),
                                cl.getContactBy().getId())
                ));

        grouped.forEach((key, logs) -> {
            String[] parts = key.split("_");
            ContactChannel contactChannel = ContactChannel.valueOf(parts[0]);
            UUID departmentId = UUID.fromString(parts[1]);
            UUID userId = UUID.fromString(parts[2]);

            int total = logs.size();
            int successful = (int) logs.stream()
                    .filter(cl -> cl.getContactOutcome() ==
                            ContactOutcome.SUCCESSFUL).count();


            int failed = (int) logs.stream()
                    .filter(cl -> cl.getContactOutcome() == ContactOutcome.NO_ANSWER
                            || cl.getContactOutcome() == ContactOutcome.NOT_INTERESTED)
                    .count();


            int pending = (int) logs.stream()
                    .filter(cl -> cl.getNextFollowUp() != null
                            && cl.getNextFollowUp().isAfter(ChronoLocalDate.from(LocalDateTime.now())))
                    .count();

            BigDecimal successfulRate = total > 0
                    ? BigDecimal.valueOf((double) successful / total * 100)
                    : BigDecimal.ZERO;

            SalesMetrics metrics = new SalesMetrics();
            metrics.setId(UUID.randomUUID());
            metrics.setPeriod(date);
            metrics.setContactChannel(contactChannel);
            metrics.setDepartment(departmentRepository.findById(departmentId).orElse(null));
            metrics.setUserId(userRepository.findById(userId).orElse(null));
            metrics.setTotalContact(total);
            metrics.setSuccessfulContact(successful);
            metrics.setFailedContact(failed);
            metrics.setPendingFollowUp(pending);
            metrics.setSuccessRate(successfulRate);
            metrics.setConversionRate(successfulRate);

            salesMetricsRepository.save(metrics);
        });
    }

    @Override
    @Transactional
    public void clearAllMetrics() {

        salesMetricsRepository.deleteAll();
    }
}
