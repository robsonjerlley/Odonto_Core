package io.sertaoBit.odontocore.crm.modules.analytics.service.impl;

import io.sertaoBit.odontocore.crm.core.enums.*;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.*;
import io.sertaoBit.odontocore.crm.modules.analytics.service.AnalyticsService;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.AdsInvestmentRepository;
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
import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final CustomerRepository customerRepository;
    private final LeadTicketRepository leadTicketRepository;
    private final DealRepository dealRepository;
    private final AdsInvestmentRepository adsInvestmentRepository;
    private final UserRepository userRepository;
    private final BonusConfigRepository bonusConfigRepository;
    private final PermissionService permissionService;

    public AnalyticsServiceImpl(
            CustomerRepository customerRepository,
            LeadTicketRepository leadTicketRepository,
            DealRepository dealRepository,
            AdsInvestmentRepository adsInvestmentRepository,
            UserRepository userRepository,
            BonusConfigRepository bonusConfigRepository,
            PermissionService permissionService
    ) {
        this.customerRepository = customerRepository;
        this.leadTicketRepository = leadTicketRepository;
        this.dealRepository = dealRepository;
        this.adsInvestmentRepository = adsInvestmentRepository;
        this.userRepository = userRepository;
        this.bonusConfigRepository = bonusConfigRepository;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public AdsRoiResultDTO getAdsRoi(AdsChannel channel, DataRangeDTO period, UUID userId) {

        var currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        permissionService.checkOrThrow(
                currentUser, Resource.ANALYTICS,Action.READ,
                null , null
        );

        List<Customer> customers = customerRepository.findByChannel(channel);
        List<UUID>  customersId = customers.stream().map(Customer::getId).toList();
        List<LeadTicket> tickets = leadTicketRepository.findByStatus(TicketStatus.WIN);
        if(tickets.getStatus() ) {}



        return null;
    }

    @Override
    public StageConversionResultDTO getConversionByStage(DataRangeDTO period, Sector sector, UUID userId) {
        return null;
    }

    @Override
    public List<SectorDropOffResultDTO> getDropOffBySector(DataRangeDTO period, UUID userId) {
        return List.of();
    }

    @Override
    public UserPerformanceResultDTO getUserPerformance(UUID targetUserId, DataRangeDTO period, UUID userId) {
        return null;
    }

    @Override
    public BigDecimal getCalculatedBonus(UUID targetId, String periodRef, UUID userId) {
        return null;
    }

    @Override
    public GlobalDashBoardResultDTO getGlobalDashBoard(DataRangeDTO period, UUID userId) {
        return null;
    }
}
