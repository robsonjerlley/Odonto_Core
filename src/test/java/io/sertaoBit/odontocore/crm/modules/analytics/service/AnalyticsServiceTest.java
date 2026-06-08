package io.sertaoBit.odontocore.crm.modules.analytics.service;

import io.sertaoBit.odontocore.crm.core.enums.*;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.*;
import io.sertaoBit.odontocore.crm.modules.analytics.service.impl.AnalyticsServiceImpl;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.BonusConfigRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.service.ConfigService;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService - Testes Unitários")
public class AnalyticsServiceTest {

    private AnalyticsService analyticsService;
    @Mock private CustomerRepository customerRepository;
    @Mock private LeadTicketRepository leadTicketRepository;
    @Mock private DealRepository dealRepository;
    @Mock private UserRepository userRepository;
    @Mock private BonusConfigRepository bonusConfigRepository;
    @Mock private ConfigService configService;
    @Mock private PermissionService permissionService;

    private final DataRangeDTO period = new DataRangeDTO(
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31)
    );

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(
                customerRepository, leadTicketRepository, dealRepository,
                userRepository, bonusConfigRepository, configService, permissionService);
    }

    private User buildUser(UUID id, Sector sector, Role role) {
        return User.builder()
                .id(id)
                .name("Test")
                .username("test@test.com")
                .passwordHash("hash")
                .sector(sector)
                .role(role)
                .active(true)
                .build();
    }

    private Deal buildClosedDeal(UUID closedBy, BigDecimal finalValue, PaymentMethod paymentMethod) {
        return Deal.builder()
                .id(UUID.randomUUID())
                .closedBy(closedBy)
                .finalValue(finalValue)
                .paymentMethod(paymentMethod)
                .archived(false)
                .build();
    }

    // -------------------------------------------------------------------------
    // getAdsRoi
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao calcular ROI com usuário inexistente")
    void getAdsRoi_userNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> analyticsService.getAdsRoi(AdsChannel.META, period, userId));
    }

    @Test
    @DisplayName("Deve retornar roiMultiplier zero quando não há investimento no período")
    void getAdsRoi_zeroInvestment_roiIsZero() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Sector.LEADS, Role.ADM_LEADS);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerRepository.findByAdsChannel(AdsChannel.META)).thenReturn(List.of());
        when(leadTicketRepository.findByCustomerIdInAndStatusAndClosedAtBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(dealRepository.findByTicketIdIn(any())).thenReturn(List.of());
        when(configService.sumInvestmentByChannelAndPeriod(any(), any())).thenReturn(BigDecimal.ZERO);

        AdsRoiResultDTO result = analyticsService.getAdsRoi(AdsChannel.META, period, userId);

        assertEquals(BigDecimal.ZERO, result.roiMultiplier());
        assertEquals(BigDecimal.ZERO, result.totalRevenue());
    }

    // -------------------------------------------------------------------------
    // getCalculatedBonus
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar BigDecimal.ZERO quando não há BonusConfig para o usuário")
    void getCalculatedBonus_noConfig_returnsZero() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        User user = buildUser(userId, Sector.LEADS, Role.ADM_LEADS);
        User target = buildUser(targetId, Sector.COMMERCIAL, Role.USER_COMMERCIAL);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any()))
                .thenReturn(Optional.empty());

        BonusResultDTO result = analyticsService.getCalculatedBonus(targetId, "2026-05", userId);

        assertEquals(BigDecimal.ZERO, result.value());
    }

    // -------------------------------------------------------------------------
    // getConversionByStage
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve calcular percentuais de conversão por estágio corretamente")
    void getConversionByStage_correctPercentages() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Sector.LEADS, Role.ADM_LEADS);

        LeadTicket t1 = LeadTicket.builder().id(UUID.randomUUID()).status(TicketStatus.NEW).build();
        LeadTicket t2 = LeadTicket.builder().id(UUID.randomUUID()).status(TicketStatus.NEW).build();
        LeadTicket t3 = LeadTicket.builder().id(UUID.randomUUID())
                .status(TicketStatus.NEGOTIATION)
                .scheduledAt(LocalDateTime.now())
                .build();
        LeadTicket t4 = LeadTicket.builder().id(UUID.randomUUID())
                .status(TicketStatus.WIN)
                .scheduledAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leadTicketRepository.findByCreatedAtBetween(any(), any()))
                .thenReturn(List.of(t1, t2, t3, t4));

        StageConversionResultDTO result = analyticsService.getConversionByStage(period, null, userId);

        assertEquals(4L, result.captureCount());
        assertEquals(2L, result.scheduledCount());
        assertEquals(2L, result.dealCreatedCount());
        assertEquals(1L, result.closedCount());
        assertEquals(new BigDecimal("50.00"), result.leadsConversionPct());
        assertEquals(new BigDecimal("100.00"), result.evaluationConversionPct());
        assertEquals(new BigDecimal("50.00"), result.commercialConversionPct());
    }

    // -------------------------------------------------------------------------
    // getUserPerformance — expectedCash
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve calcular expectedCash aplicando conversionFactor para vendedor COMMERCIAL")
    void getUserPerformance_commercial_calculatesExpectedCash() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        User requester = buildUser(userId, Sector.LEADS, Role.ADM_LEADS);
        User seller = buildUser(sellerId, Sector.COMMERCIAL, Role.USER_COMMERCIAL);

        // PIX: 5000 * 1.00 = 5000.00
        // CREDIT_CARD: 3000 * 0.97 = 2910.00
        // total esperado: 7910.00
        Deal dealPix = buildClosedDeal(sellerId, new BigDecimal("5000.00"), PaymentMethod.PIX);
        Deal dealCredit = buildClosedDeal(sellerId, new BigDecimal("3000.00"), PaymentMethod.CREDIT_CARD);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(dealRepository.findByClosedByAndClosedAtBetween(eq(sellerId), any(), any()))
                .thenReturn(List.of(dealPix, dealCredit));
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any()))
                .thenReturn(Optional.empty());

        UserPerformanceResultDTO result = analyticsService.getUserPerformance(sellerId, period, userId);

        assertEquals(new BigDecimal("7910.00"), result.expectedCash());
    }

    @Test
    @DisplayName("Deve retornar expectedCash zero para usuário não-COMMERCIAL")
    void getUserPerformance_nonCommercial_expectedCashIsZero() {
        UUID userId = UUID.randomUUID();
        UUID attendantId = UUID.randomUUID();
        User requester = buildUser(userId, Sector.LEADS, Role.ADM_LEADS);
        User attendant = buildUser(attendantId, Sector.ATTENDANT, Role.USER_ATTENDANT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(userRepository.findById(attendantId)).thenReturn(Optional.of(attendant));
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any()))
                .thenReturn(Optional.empty());

        UserPerformanceResultDTO result = analyticsService.getUserPerformance(attendantId, period, userId);

        assertEquals(BigDecimal.ZERO, result.expectedCash());
    }

    @Test
    @DisplayName("Deve ignorar deals sem paymentMethod no cálculo de expectedCash")
    void getUserPerformance_commercial_ignoresDealWithoutPaymentMethod() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        User requester = buildUser(userId, Sector.LEADS, Role.ADM_LEADS);
        User seller = buildUser(sellerId, Sector.COMMERCIAL, Role.USER_COMMERCIAL);

        Deal dealComPayment = buildClosedDeal(sellerId, new BigDecimal("4000.00"), PaymentMethod.CASH);
        Deal dealSemPayment = Deal.builder()
                .id(UUID.randomUUID())
                .closedBy(sellerId)
                .finalValue(new BigDecimal("2000.00"))
                .paymentMethod(null)
                .archived(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(dealRepository.findByClosedByAndClosedAtBetween(eq(sellerId), any(), any()))
                .thenReturn(List.of(dealComPayment, dealSemPayment));
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any()))
                .thenReturn(Optional.empty());

        UserPerformanceResultDTO result = analyticsService.getUserPerformance(sellerId, period, userId);

        // apenas o deal com CASH (1.00) é contabilizado: 4000 * 1.00 = 4000.00
        assertEquals(new BigDecimal("4000.00"), result.expectedCash());
    }

    // -------------------------------------------------------------------------
    // getGlobalDashboard — totalExpectedCash
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve calcular totalExpectedCash no dashboard global somando todos os deals fechados")
    void getGlobalDashboard_calculatesTotalExpectedCash() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Sector.LEADS, Role.ADM_LEADS);

        // INSTALLMENT: 10000 * 0.85 = 8500.00
        // DENTAL_INSURANCE: 5000 * 0.90 = 4500.00
        // total esperado: 13000.00
        Deal d1 = buildClosedDeal(UUID.randomUUID(), new BigDecimal("10000.00"), PaymentMethod.INSTALLMENT);
        Deal d2 = buildClosedDeal(UUID.randomUUID(), new BigDecimal("5000.00"), PaymentMethod.DENTAL_INSURANCE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerRepository.findByAdsChannel(any())).thenReturn(List.of());
        when(leadTicketRepository.findByCustomerIdInAndStatusAndClosedAtBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(dealRepository.findByTicketIdIn(any())).thenReturn(List.of());
        when(configService.sumInvestmentByChannelAndPeriod(any(), any())).thenReturn(BigDecimal.ZERO);
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(userRepository.findByActiveTrue()).thenReturn(List.of());
        when(dealRepository.findByClosedAtBetweenAndArchivedFalse(any(), any())).thenReturn(List.of(d1, d2));

        GlobalDashBoardResultDTO result = analyticsService.getGlobalDashBoard(period, userId);

        assertEquals(new BigDecimal("13000.00"), result.totalExpectedCash());
    }

    @Test
    @DisplayName("Deve retornar totalExpectedCash zero quando não há deals fechados no período")
    void getGlobalDashboard_noDeals_totalExpectedCashIsZero() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Sector.LEADS, Role.ADM_LEADS);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerRepository.findByAdsChannel(any())).thenReturn(List.of());
        when(leadTicketRepository.findByCustomerIdInAndStatusAndClosedAtBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(dealRepository.findByTicketIdIn(any())).thenReturn(List.of());
        when(configService.sumInvestmentByChannelAndPeriod(any(), any())).thenReturn(BigDecimal.ZERO);
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(userRepository.findByActiveTrue()).thenReturn(List.of());
        when(dealRepository.findByClosedAtBetweenAndArchivedFalse(any(), any())).thenReturn(List.of());

        GlobalDashBoardResultDTO result = analyticsService.getGlobalDashBoard(period, userId);

        assertEquals(new BigDecimal("0.00"), result.totalExpectedCash());
    }
}
