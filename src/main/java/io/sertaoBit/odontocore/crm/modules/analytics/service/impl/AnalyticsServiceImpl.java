package io.sertaoBit.odontocore.crm.modules.analytics.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.*;
import io.sertaoBit.odontocore.crm.modules.analytics.service.AnalyticsService;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.BonusConfigRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.service.ConfigService;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.sertaoBit.odontocore.crm.core.enums.Action.READ;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.ANALYTICS;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.*;
import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.*;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final CustomerRepository customerRepository;
    private final LeadTicketRepository leadTicketRepository;
    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final BonusConfigRepository bonusConfigRepository;
    private final ConfigService configService;
    private final PermissionService permissionService;
    private final SecurityUtils securityUtils;

    public AnalyticsServiceImpl(
            CustomerRepository customerRepository,
            LeadTicketRepository leadTicketRepository,
            DealRepository dealRepository,
            UserRepository userRepository,
            BonusConfigRepository bonusConfigRepository,
            ConfigService configService,
            PermissionService permissionService,
            SecurityUtils securityUtils
    ) {
        this.customerRepository = customerRepository;
        this.leadTicketRepository = leadTicketRepository;
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.bonusConfigRepository = bonusConfigRepository;
        this.configService = configService;
        this.permissionService = permissionService;
        this.securityUtils = securityUtils;
    }


    @Override
    public StageConversionResultDTO getConversionByStage(DataRangeDTO period, Sector sector) {
        User user = securityUtils.getCurrentUser();
        PermissionScope scope = permissionService.getScope(
                user,
                ANALYTICS,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access denied"));

        Sector effectiveSector = scope == PermissionScope.SECTOR
                ? user.getSector()
                : sector;

        List<LeadTicket> tickets = leadTicketRepository.findByCreatedAtBetween(
                period.from().atStartOfDay(),
                period.to().atTime(23, 59, 59)
        );

        if (effectiveSector != null) {
            tickets = tickets.stream()
                    .filter(t -> t.getCurrentSector() == effectiveSector)
                    .toList();

        }

        var dealStatuses = Set.of(
                NEGOTIATION,
                WIN,
                PENDING,
                RECYCLED
        );

        long captureCount = tickets.size();
        var scheduledStatuses = Set.of(SCHEDULED, IN_EVALUATION, NEGOTIATION, WIN, PENDING, RECYCLED, POST_PROCEDURE);
        long scheduledCount = tickets.stream().filter(t -> scheduledStatuses.contains(t.getStatus())).count();
        long dealCreatedCount = tickets.stream().filter(t -> dealStatuses.contains(t.getStatus())).count();
        long closedCount = tickets.stream().filter(t -> t.getStatus() == WIN).count();

        BigDecimal leadsConversionPct = captureCount == 0 ? ZERO
                : valueOf(scheduledCount * 100)
                .divide(valueOf(captureCount), 2, RoundingMode.HALF_UP);

        BigDecimal evaluationConversionPct = scheduledCount == 0 ? ZERO
                : valueOf(dealCreatedCount * 100)
                .divide(valueOf(scheduledCount), 2, RoundingMode.HALF_UP);

        BigDecimal commercialConversionPct = dealCreatedCount == 0 ? ZERO
                : valueOf(closedCount * 100)
                .divide(valueOf(dealCreatedCount), 2, RoundingMode.HALF_UP);

        return new StageConversionResultDTO(
                effectiveSector,
                captureCount,
                scheduledCount,
                dealCreatedCount,
                closedCount,
                leadsConversionPct,
                evaluationConversionPct,
                commercialConversionPct
        );
    }

    @Override
    public List<SectorDropOffResultDTO> getDropOffBySector(DataRangeDTO period) {
        User user = securityUtils.getCurrentUser();
        PermissionScope scope = permissionService.getScope(
                user,
                ANALYTICS,
                READ

        ).orElseThrow(() -> new AccessDeniedException("Access denied"));

        List<LeadTicket> tickets = leadTicketRepository.findByCreatedAtBetween(
                period.from().atStartOfDay(),
                period.to().atTime(23, 59, 59)
        );


        var commercialStatuses = Set.of(
                NEGOTIATION, WIN,
                PENDING, RECYCLED
        );


        long leadsEntry = tickets.size();
        long leadsLoss = tickets.stream()
                .filter(t -> t.getStatus() == LOSS
                        && t.getCurrentSector() == LEADS)
                .count();

        long evaluatorEntry = tickets.stream()
                .filter(t -> t.getScheduledAt() != null)
                .count();
        long evaluatorLoss = tickets.stream()
                .filter(t -> t.getStatus() == LOSS
                        && t.getCurrentSector() == EVALUATOR)
                .count();

        long commercialEntry = tickets.stream()
                .filter(t -> commercialStatuses.contains(t.getStatus())
                        || (t.getStatus() == LOSS
                        && t.getCurrentSector() == COMMERCIAL))
                .count();
        long commercialLoss = tickets.stream()
                .filter(t -> t.getStatus() == LOSS
                        && t.getCurrentSector() == COMMERCIAL)
                .count();

        List<SectorDropOffResultDTO> result = List.of(
                buildDropOff(LEADS, leadsEntry, leadsLoss),
                buildDropOff(EVALUATOR, evaluatorEntry, evaluatorLoss),
                buildDropOff(COMMERCIAL, commercialEntry, commercialLoss)
        );

        return scope ==  PermissionScope.SECTOR
                ? result.stream().filter(
                        r -> r.sector() == user.getSector()
        ).toList() : result;

    }


    @Override
    public UserPerformanceResultDTO getUserPerformance(UUID targetUserId, DataRangeDTO period) {
        User user = securityUtils.getCurrentUser();
        PermissionScope scope = permissionService.getScope(
                user,
                ANALYTICS,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access denied"));

        if(scope == PermissionScope.OWN && !user.getId().equals(targetUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        var targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        if (!YearMonth.from(period.from()).equals(YearMonth.from(period.to()))) {
            throw new IllegalArgumentException(
                    "Analytics de performance carrega bônus mensal: o range deve estar contido em um único mês calendário.");
        }

        String periodRef = period.from().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal calculatedBonus = getCalculatedBonus(targetUser.getId(), periodRef).value();

        var target = computePerformance(targetUser,period);
        return new UserPerformanceResultDTO(
                target.userId(),
                target.name(),
                target.sector(),
                target.totalAssigned(),
                target.totalConverted(),
                target.conversionPct(),
                target.avgTicketValue(),
                target.expectedCash(),
                calculatedBonus,
                periodRef
        );
    }


    @Override
    public GlobalDashBoardResultDTO getGlobalDashBoard(DataRangeDTO period) {
        User user = securityUtils.getCurrentUser();
        PermissionScope scope = permissionService.getScope(
                user,
                ANALYTICS,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access denied"));

        if(scope != PermissionScope.GLOBAL) {
            throw new AccessDeniedException("Access denied");
        }

        var adsRoiList = Arrays.stream(AdsChannel.values())
                .map(channel -> getAdsRoi(channel, period))
                .toList();

        var stageConversion = getConversionByStage(period, null);
        var sectorDropOff = getDropOffBySector(period);

        var topPerformers = userRepository.findByActiveTrue().stream()
                .map(u -> computePerformance(u, period))
                .toList();

        var postProcedures = getPostProcedureMetrics(period);

        var from = period.from().atStartOfDay();
        var to = period.to().atTime(23, 59, 59);
        BigDecimal totalExpectedCash = dealRepository.findByClosedAtBetweenAndArchivedFalse(from, to).stream()
                .filter(d -> d.getFinalValue() != null && d.getPaymentMethod() != null)
                .map(d -> d.getFinalValue().multiply(d.getPaymentMethod().getConversionFactor()))
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new GlobalDashBoardResultDTO(
                period,
                adsRoiList,
                stageConversion,
                sectorDropOff,
                topPerformers,
                postProcedures,
                totalExpectedCash
        );
    }


    private UserPerformanceResultDTO computePerformance(User targetUser, DataRangeDTO period) {

        long totalAssigned;
        long totalConverted;
        BigDecimal avgTicketValue = ZERO;
        BigDecimal expectedCash = ZERO;

        var periodFrom = period.from().atStartOfDay();
        var periodTo = period.to().atTime(23, 59, 59);

        var sector = targetUser.getSector();
        if (sector == LEADS || sector == ATTENDANT) {
            var tickets = leadTicketRepository.findByCreatedAtBetween(periodFrom, periodTo).stream()
                    .filter(t -> targetUser.getId().equals(t.getAssignedTo()))
                    .toList();
            totalAssigned = tickets.size();
            totalConverted = tickets.stream().filter(t -> t.getScheduledAt() != null).count();

        } else if (sector == EVALUATOR) {
            var deals = dealRepository.findByCreatedByAndCreatedAtBetween(targetUser.getId(), periodFrom, periodTo);
            totalAssigned = deals.size();
            totalConverted = deals.stream().filter(d -> d.getFinalValue() != null).count();

        } else {
            var closedDeals = dealRepository.findByClosedByAndClosedAtBetween(targetUser.getId(), periodFrom, periodTo);
            totalAssigned = closedDeals.size();
            totalConverted = closedDeals.stream().filter(d -> d.getFinalValue() != null).count();
            if (!closedDeals.isEmpty()) {
                avgTicketValue = closedDeals.stream()
                        .map(d -> d.getFinalValue() != null ? d.getFinalValue() : ZERO)
                        .reduce(ZERO, BigDecimal::add)
                        .divide(valueOf(closedDeals.size()), 2, RoundingMode.HALF_UP);
                expectedCash = closedDeals.stream()
                        .filter(d -> d.getFinalValue() != null && d.getPaymentMethod() != null)
                        .map(d -> d.getFinalValue().multiply(d.getPaymentMethod().getConversionFactor()))
                        .reduce(ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal conversionPct = totalAssigned == 0 ? ZERO
                : valueOf(totalConverted * 100)
                .divide(valueOf(totalAssigned), 2, RoundingMode.HALF_UP);

        return new UserPerformanceResultDTO(
                targetUser.getId(),
                targetUser.getName(),
                targetUser.getSector(),
                totalAssigned,
                totalConverted,
                conversionPct,
                avgTicketValue,
                expectedCash,
                ZERO,
                null
        );

    }


    private BonusResultDTO getCalculatedBonus(UUID targetId, String periodRef) {

        var targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        var bonusConfig = bonusConfigRepository
                .findByRoleAndSectorAndPeriodRef(targetUser.getRole(), targetUser.getSector(), periodRef)
                .orElse(null);

        if (bonusConfig == null || !bonusConfig.isActive()) return new BonusResultDTO(ZERO);

        var yearMonth = YearMonth.parse(periodRef);
        var period = new DataRangeDTO(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        long metricValue = resolveMetric(targetUser, period);

        var calculated = valueOf(metricValue)
                .multiply(bonusConfig.getBonusPct())
                .divide(valueOf(100), 2, RoundingMode.HALF_UP);

        return new BonusResultDTO(calculated);
    }


    private AdsRoiResultDTO getAdsRoi(AdsChannel channel, DataRangeDTO period) {

        List<Customer> customers = customerRepository.findByAdsChannel(channel);
        List<UUID> customersId = customers.stream().map(Customer::getId).toList();

        List<LeadTicket> tickets = leadTicketRepository.findByCustomerIdInAndStatusAndClosedAtBetween(
                customersId, WIN,
                period.from().atStartOfDay(),
                period.to().atTime(23, 59, 59)
        );

        List<UUID> ticketsIds = tickets.stream().map(LeadTicket::getId).toList();
        List<Deal> closeDeals = dealRepository.findByTicketIdIn(ticketsIds).stream()
                .filter(deal -> !deal.isArchived()).toList();

        BigDecimal totalRevenue = closeDeals.stream()
                .map(d -> d.getFinalValue() != null ? d.getFinalValue() : ZERO)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalInvestment = configService.sumInvestmentByChannelAndPeriod(channel, period);

        BigDecimal roiMultiplier = totalInvestment.compareTo(ZERO) == 0
                ? ZERO
                : totalRevenue.divide(totalInvestment, 2, RoundingMode.HALF_UP);

        return new AdsRoiResultDTO(
                channel,
                totalInvestment,
                totalRevenue,
                roiMultiplier,
                (long) customers.size(),
                (long) tickets.size()
        );


    }



    private PostProcedureResultDTO getPostProcedureMetrics(DataRangeDTO period) {

        var from = period.from().atStartOfDay();
        var to = period.to().atTime(23, 59, 59);

        var procedures = leadTicketRepository.findByProcedurePerformedAtBetween(from, to);

        var totalProcedures = procedures.size();

        var returnCount = procedures.stream()
                .filter(p -> p.getStatus().equals(SCHEDULED)).count();

        var lostCount = procedures.stream()
                .filter(p -> p.getStatus().equals(LOSS)).count();

        var pendingCount = procedures.stream()
                .filter(p -> p.getStatus().equals(POST_PROCEDURE)).count();

        BigDecimal returnRate = totalProcedures == 0 ? ZERO
                : valueOf(returnCount * 100)
                .divide(valueOf(totalProcedures), 2, RoundingMode.HALF_UP);

        return new PostProcedureResultDTO(totalProcedures, (int) returnCount, (int) lostCount, returnRate, (int) pendingCount);
    }


    private SectorDropOffResultDTO buildDropOff(Sector sector, long entryCount, long lossCount) {
        long exitCount = entryCount - lossCount;
        BigDecimal dropOffPct = entryCount == 0 ? ZERO
                : valueOf(lossCount * 100)
                .divide(valueOf(entryCount), 2, RoundingMode.HALF_UP);
        return new SectorDropOffResultDTO(sector, entryCount, exitCount, lossCount, dropOffPct);


    }


    private long resolveMetric(User targetUser, DataRangeDTO period) {
        var from = period.from().atStartOfDay();
        var to = period.to().atTime(23, 59, 59);
        var sector = targetUser.getSector();
        if (sector == LEADS || sector == ATTENDANT) {
            return leadTicketRepository.findByCreatedAtBetween(from, to).stream()
                    .filter(t -> targetUser.getId().equals(t.getAssignedTo()) && t.getScheduledAt() != null)
                    .count();
        } else if (sector == EVALUATOR) {
            return dealRepository.findByCreatedByAndCreatedAtBetween(targetUser.getId(), from, to).size();
        } else {
            return dealRepository.findByClosedByAndClosedAtBetween(targetUser.getId(), from, to).size();
        }
    }
}
