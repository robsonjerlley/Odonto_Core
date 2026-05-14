package io.sertaoBit.odontocore.crm.modules.analytics.service.impl;

import io.sertaoBit.odontocore.crm.core.enums.*;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.*;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.service.ConfigService;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.service.AnalyticsService;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.BonusConfigRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    public AnalyticsServiceImpl(
            CustomerRepository customerRepository,
            LeadTicketRepository leadTicketRepository,
            DealRepository dealRepository,
            UserRepository userRepository,
            BonusConfigRepository bonusConfigRepository,
            ConfigService configService,
            PermissionService permissionService
    ) {
        this.customerRepository = customerRepository;
        this.leadTicketRepository = leadTicketRepository;
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.bonusConfigRepository = bonusConfigRepository;
        this.configService = configService;
        this.permissionService = permissionService;
    }

    @Override
    public AdsRoiResultDTO getAdsRoi(AdsChannel channel, DataRangeDTO period, UUID userId) {

        var currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        permissionService.checkOrThrow(
                currentUser, Resource.ANALYTICS,Action.READ,
                null , null
        );

        List<Customer> customers = customerRepository.findByAdChannel(channel);
        List<UUID>  customersId = customers.stream().map(Customer::getId).toList();

        List<LeadTicket> tickets = leadTicketRepository.findByCustomerIdInAndStatusAndClosedAtBetween(
           customersId, TicketStatus.WIN,
                period.from().atStartOfDay(),
                period.to().atTime(23,59,59)
        );

        List<UUID> ticketsIds = tickets.stream().map(LeadTicket::getId).toList();
        List<Deal> closeDeals =  dealRepository.findByTicketIdIn(ticketsIds).stream()
                .filter(deal -> !deal.isArchived()).toList();

        BigDecimal totalRevenue = closeDeals.stream()
                .map(d-> d.getFinalValue() != null ? d.getFinalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInvestment = configService.sumInvestmentByChannelAndPeriod(channel, period);

        BigDecimal roiMultiplier = totalInvestment.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(totalInvestment, 2 , RoundingMode.HALF_UP);

        return new AdsRoiResultDTO(
              channel,
              totalInvestment,
              totalRevenue,
              roiMultiplier,
                (long) customers.size(),
                (long) tickets.size()
        );


    }

    @Override
    public StageConversionResultDTO getConversionByStage(DataRangeDTO period, Sector sector, UUID userId) {
        var currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        permissionService.checkOrThrow(
                currentUser, Resource.ANALYTICS,Action.READ,
                null , null
        );

        List<LeadTicket> tickets = leadTicketRepository.findByCreatedAtBetween(
                period.from().atStartOfDay(),
                period.to().atTime(23,59,59)
        );

        if(sector != null) {
            tickets = tickets.stream()
                    .filter(t -> t.getCurrentSector() == sector)
                    .toList();

        }

        var dealStatuses = Set.of(
                TicketStatus.NEGOTIATION,
                TicketStatus.WIN,
                TicketStatus.PENDING,
                TicketStatus.RECYCLED
        );

        long captureCount = tickets.size();
        long scheduledCount = tickets.stream().filter(t -> t.getScheduledAt() != null).count();
        long dealCreatedCount = tickets.stream().filter(t -> dealStatuses.contains(t.getStatus())).count();
        long closedCount = tickets.stream().filter(t-> t.getStatus() == TicketStatus.WIN).count();

        BigDecimal leadsConversionPct = captureCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(scheduledCount * 100)
                  .divide(BigDecimal.valueOf(captureCount), 2, RoundingMode.HALF_UP);

        BigDecimal evaluationConversionPct = scheduledCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(dealCreatedCount * 100)
                  .divide(BigDecimal.valueOf(scheduledCount), 2, RoundingMode.HALF_UP);

        BigDecimal commercialConversionPct = dealCreatedCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(closedCount * 100)
                  .divide(BigDecimal.valueOf(dealCreatedCount), 2, RoundingMode.HALF_UP);

        return new StageConversionResultDTO(
                sector,
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
    public List<SectorDropOffResultDTO> getDropOffBySector(DataRangeDTO period, UUID userId) {
        var currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        permissionService.checkOrThrow(
                currentUser, Resource.ANALYTICS,Action.READ,
                null , null
        );

        List<LeadTicket> tickets = leadTicketRepository.findByCreatedAtBetween(
                period.from().atStartOfDay(),
                period.to().atTime(23, 59, 59)
        );


        var commercialStatuses = Set.of(
                TicketStatus.NEGOTIATION, TicketStatus.WIN,
                TicketStatus.PENDING, TicketStatus.RECYCLED
        );


        long leadsEntry = tickets.size();
        long leadsLoss  = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.LOSS
                        && t.getCurrentSector() == Sector.LEADS)
                .count();

        long evaluatorEntry = tickets.stream()
                .filter(t -> t.getScheduledAt() != null)
                .count();
        long evaluatorLoss  = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.LOSS
                        && t.getCurrentSector() == Sector.EVALUATOR)
                .count();

        long commercialEntry = tickets.stream()
                .filter(t -> commercialStatuses.contains(t.getStatus())
                        || (t.getStatus() == TicketStatus.LOSS
                        && t.getCurrentSector() == Sector.COMMERCIAL))
                .count();
        long commercialLoss  = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.LOSS
                        && t.getCurrentSector() == Sector.COMMERCIAL)
                .count();

        return List.of(
                buildDropOff(Sector.LEADS,      leadsEntry,      leadsLoss),
                buildDropOff(Sector.EVALUATOR,  evaluatorEntry,  evaluatorLoss),
                buildDropOff(Sector.COMMERCIAL, commercialEntry, commercialLoss)
        );
    }

    private SectorDropOffResultDTO buildDropOff(Sector sector, long entryCount, long lossCount) {
        long exitCount = entryCount - lossCount;
        BigDecimal dropOffPct = entryCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(lossCount * 100)
                  .divide(BigDecimal.valueOf(entryCount), 2, RoundingMode.HALF_UP);
        return new SectorDropOffResultDTO(sector, entryCount, exitCount, lossCount, dropOffPct);


    }

    @Override
    public UserPerformanceResultDTO getUserPerformance(UUID targetUserId, DataRangeDTO period, UUID userId) {
        var currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        permissionService.checkOrThrow(currentUser, Resource.ANALYTICS, Action.READ, null, null);

        var targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        var from = period.from().atStartOfDay();
        var to   = period.to().atTime(23, 59, 59);

        long totalAssigned;
        long totalConverted;
        BigDecimal avgTicketValue = BigDecimal.ZERO;

        var sector = targetUser.getSector();
        if (sector == Sector.LEADS || sector == Sector.ATTENDANT) {
            var tickets = leadTicketRepository.findByCreatedAtBetween(from, to).stream()
                    .filter(t -> targetUser.getId().equals(t.getAssignedTo()))
                    .toList();
            totalAssigned  = tickets.size();
            totalConverted = tickets.stream().filter(t -> t.getScheduledAt() != null).count();

        } else if (sector == Sector.EVALUATOR) {
            var deals = dealRepository.findByCreatedByAndCreatedAtBetween(targetUser.getId(), from, to);
            totalAssigned  = deals.size();
            totalConverted = deals.stream().filter(d -> d.getFinalValue() != null).count();

        } else {
            var closedDeals = dealRepository.findByClosedByAndClosedAtBetween(targetUser.getId(), from, to);
            totalAssigned  = closedDeals.size();
            totalConverted = closedDeals.stream().filter(d -> d.getFinalValue() != null).count();
            if (!closedDeals.isEmpty()) {
                avgTicketValue = closedDeals.stream()
                        .map(d -> d.getFinalValue() != null ? d.getFinalValue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(closedDeals.size()), 2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal conversionPct = totalAssigned == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalConverted * 100)
                        .divide(BigDecimal.valueOf(totalAssigned), 2, RoundingMode.HALF_UP);

        String periodRef = period.from().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal calculatedBonus = getCalculatedBonus(targetUser.getId(), periodRef, userId);

        return new UserPerformanceResultDTO(
                targetUser.getId(),
                targetUser.getName(),
                targetUser.getSector(),
                totalAssigned,
                totalConverted,
                conversionPct,
                avgTicketValue,
                calculatedBonus
        );
    }

    @Override
    public BigDecimal getCalculatedBonus(UUID targetId, String periodRef, UUID userId) {
        var currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        permissionService.checkOrThrow(currentUser, Resource.ANALYTICS, Action.READ, null, null);

        var targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        var bonusConfig = bonusConfigRepository
                .findByRoleAndSectorAndPeriodRef(targetUser.getRole(), targetUser.getSector(), periodRef)
                .orElse(null);

        if (bonusConfig == null || !bonusConfig.isActive()) return BigDecimal.ZERO;

        var yearMonth = YearMonth.parse(periodRef);
        var period    = new DataRangeDTO(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        long metricValue = resolveMetric(targetUser, period);

        return BigDecimal.valueOf(metricValue)
                .multiply(bonusConfig.getBonusPct())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public GlobalDashBoardResultDTO getGlobalDashBoard(DataRangeDTO period, UUID userId) {
        var currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        permissionService.checkOrThrow(currentUser, Resource.ANALYTICS, Action.READ, null, null);

        var adsRoiList = Arrays.stream(AdsChannel.values())
                .map(channel -> getAdsRoi(channel, period, userId))
                .toList();

        var stageConversion = getConversionByStage(period, null, userId);
        var sectorDropOff   = getDropOffBySector(period, userId);

        var topPerformers = userRepository.findByActiveTrue().stream()
                .map(u -> getUserPerformance(u.getId(), period, userId))
                .toList();

        return new GlobalDashBoardResultDTO(period, adsRoiList, stageConversion, sectorDropOff, topPerformers);
    }

    private long resolveMetric(User targetUser, DataRangeDTO period) {
        var from = period.from().atStartOfDay();
        var to   = period.to().atTime(23, 59, 59);
        var sector = targetUser.getSector();
        if (sector == Sector.LEADS || sector == Sector.ATTENDANT) {
            return leadTicketRepository.findByCreatedAtBetween(from, to).stream()
                    .filter(t -> targetUser.getId().equals(t.getAssignedTo()) && t.getScheduledAt() != null)
                    .count();
        } else if (sector == Sector.EVALUATOR) {
            return dealRepository.findByCreatedByAndCreatedAtBetween(targetUser.getId(), from, to).size();
        } else {
            return dealRepository.findByClosedByAndClosedAtBetween(targetUser.getId(), from, to).size();
        }
    }
}
