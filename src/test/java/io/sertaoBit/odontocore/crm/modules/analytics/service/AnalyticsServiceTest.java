package io.sertaoBit.odontocore.crm.modules.analytics.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.*;
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
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.READ;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.ANALYTICS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
    @Mock private SecurityUtils securityUtils;

    private final DataRangeDTO period = new DataRangeDTO(
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31)
    );

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(
                customerRepository, leadTicketRepository, dealRepository,
                userRepository, bonusConfigRepository, configService, permissionService, securityUtils);
    }

    private User buildUser(UUID id, Sector sector, Role role) {
        return User.builder()
                .id(id)
                .name("Test")
                .username("test@test.com")
                .password("hash")
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
    // getConversionByStage
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getConversionByStage — GLOBAL: calcula percentuais sem filtro de setor")
    void getConversionByStage_global_calculatesPercentagesWithoutSectorFilter() {
        User user = buildUser(UUID.randomUUID(), null, Role.ADM_SYSTEM);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));

        LeadTicket t1 = LeadTicket.builder().id(UUID.randomUUID()).status(TicketStatus.NEW).build();
        LeadTicket t2 = LeadTicket.builder().id(UUID.randomUUID()).status(TicketStatus.NEW).build();
        LeadTicket t3 = LeadTicket.builder().id(UUID.randomUUID())
                .status(TicketStatus.NEGOTIATION)
                .scheduledAt(LocalDateTime.now())
                .build();
        LeadTicket t4 = LeadTicket.builder().id(UUID.randomUUID())
                .status(TicketStatus.WIN)
                .scheduledAt(LocalDateTime.now())
                .closedAt(LocalDateTime.now())
                .build();

        when(leadTicketRepository.findByCreatedAtBetween(any(), any()))
                .thenReturn(List.of(t1, t2, t3, t4));

        StageConversionResultDTO result = analyticsService.getConversionByStage(period, null);

        assertEquals(4L, result.captureCount());
        assertEquals(2L, result.scheduledCount());
        assertEquals(2L, result.dealCreatedCount());
        assertEquals(1L, result.closedCount());
        assertEquals(new BigDecimal("50.00"), result.leadsConversionPct());
        assertEquals(new BigDecimal("100.00"), result.evaluationConversionPct());
        assertEquals(new BigDecimal("50.00"), result.commercialConversionPct());
    }

    @Test
    @DisplayName("getConversionByStage — SECTOR: usa setor do usuário, ignora parâmetro recebido")
    void getConversionByStage_sector_usesUserSectorIgnoresParam() {
        User user = buildUser(UUID.randomUUID(), Sector.LEADS, Role.ADM_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.SECTOR));

        LeadTicket leadsTicket1 = LeadTicket.builder().id(UUID.randomUUID())
                .status(TicketStatus.NEW).currentSector(Sector.LEADS).build();
        LeadTicket leadsTicket2 = LeadTicket.builder().id(UUID.randomUUID())
                .status(TicketStatus.NEW).currentSector(Sector.LEADS).build();
        LeadTicket evaluatorTicket = LeadTicket.builder().id(UUID.randomUUID())
                .status(TicketStatus.NEW).currentSector(Sector.EVALUATOR).build();

        when(leadTicketRepository.findByCreatedAtBetween(any(), any()))
                .thenReturn(List.of(leadsTicket1, leadsTicket2, evaluatorTicket));

        // EVALUATOR como parâmetro — deve ser ignorado com escopo SECTOR; usa setor do usuário (LEADS)
        StageConversionResultDTO result = analyticsService.getConversionByStage(period, Sector.EVALUATOR);

        assertEquals(2L, result.captureCount());
        assertEquals(Sector.LEADS, result.sector());
    }

    @Test
    @DisplayName("getConversionByStage — sem permissão: lança AccessDeniedException")
    void getConversionByStage_noPermission_throwsAccessDenied() {
        User user = buildUser(UUID.randomUUID(), Sector.LEADS, Role.USER_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> analyticsService.getConversionByStage(period, null));
    }

    // -------------------------------------------------------------------------
    // getDropOffBySector
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDropOffBySector — GLOBAL: retorna os 3 setores")
    void getDropOffBySector_global_returnsAllThreeSectors() {
        User user = buildUser(UUID.randomUUID(), null, Role.ADM_SYSTEM);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());

        List<SectorDropOffResultDTO> result = analyticsService.getDropOffBySector(period);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("getDropOffBySector — SECTOR: retorna apenas o setor do usuário")
    void getDropOffBySector_sector_returnsOnlyUserSector() {
        User user = buildUser(UUID.randomUUID(), Sector.LEADS, Role.ADM_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.SECTOR));
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());

        List<SectorDropOffResultDTO> result = analyticsService.getDropOffBySector(period);

        assertEquals(1, result.size());
        assertEquals(Sector.LEADS, result.get(0).sector());
    }

    @Test
    @DisplayName("getDropOffBySector — sem permissão: lança AccessDeniedException")
    void getDropOffBySector_noPermission_throwsAccessDenied() {
        User user = buildUser(UUID.randomUUID(), Sector.LEADS, Role.USER_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> analyticsService.getDropOffBySector(period));
    }

    // -------------------------------------------------------------------------
    // getUserPerformance
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUserPerformance — OWN: permite consultar a si mesmo")
    void getUserPerformance_own_allowsSelf() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Sector.LEADS, Role.USER_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.OWN));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> analyticsService.getUserPerformance(userId, period));
    }

    @Test
    @DisplayName("getUserPerformance — OWN: nega acesso a outro usuário")
    void getUserPerformance_own_deniesOtherUser() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User user = buildUser(userId, Sector.LEADS, Role.USER_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.OWN));

        assertThrows(AccessDeniedException.class,
                () -> analyticsService.getUserPerformance(otherId, period));
    }

    @Test
    @DisplayName("getUserPerformance — COMMERCIAL: calcula expectedCash aplicando conversionFactor")
    void getUserPerformance_commercial_calculatesExpectedCash() {
        UUID requesterId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        User requester = buildUser(requesterId, null, Role.ADM_SYSTEM);
        User seller = buildUser(sellerId, Sector.COMMERCIAL, Role.USER_COMMERCIAL);

        when(securityUtils.getCurrentUser()).thenReturn(requester);
        when(permissionService.getScope(requester, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        // PIX: 5000 * 1.00 = 5000.00 | CREDIT_CARD: 3000 * 0.97 = 2910.00 → total: 7910.00
        Deal dealPix = buildClosedDeal(sellerId, new BigDecimal("5000.00"), PaymentMethod.PIX);
        Deal dealCredit = buildClosedDeal(sellerId, new BigDecimal("3000.00"), PaymentMethod.CREDIT_CARD);

        when(dealRepository.findByClosedByAndClosedAtBetween(eq(sellerId), any(), any()))
                .thenReturn(List.of(dealPix, dealCredit));
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any())).thenReturn(Optional.empty());

        UserPerformanceResultDTO result = analyticsService.getUserPerformance(sellerId, period);

        assertEquals(new BigDecimal("7910.00"), result.expectedCash());
    }

    @Test
    @DisplayName("getUserPerformance — não-COMMERCIAL: expectedCash é zero")
    void getUserPerformance_nonCommercial_expectedCashIsZero() {
        UUID requesterId = UUID.randomUUID();
        UUID attendantId = UUID.randomUUID();
        User requester = buildUser(requesterId, null, Role.ADM_SYSTEM);
        User attendant = buildUser(attendantId, Sector.ATTENDANT, Role.USER_ATTENDANT);

        when(securityUtils.getCurrentUser()).thenReturn(requester);
        when(permissionService.getScope(requester, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(userRepository.findById(attendantId)).thenReturn(Optional.of(attendant));
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any())).thenReturn(Optional.empty());

        UserPerformanceResultDTO result = analyticsService.getUserPerformance(attendantId, period);

        assertEquals(BigDecimal.ZERO, result.expectedCash());
    }

    @Test
    @DisplayName("getUserPerformance — deal sem paymentMethod é ignorado no cálculo de expectedCash")
    void getUserPerformance_commercial_ignoresDealWithoutPaymentMethod() {
        UUID requesterId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        User requester = buildUser(requesterId, null, Role.ADM_SYSTEM);
        User seller = buildUser(sellerId, Sector.COMMERCIAL, Role.USER_COMMERCIAL);

        when(securityUtils.getCurrentUser()).thenReturn(requester);
        when(permissionService.getScope(requester, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        Deal dealComPayment = buildClosedDeal(sellerId, new BigDecimal("4000.00"), PaymentMethod.CASH);
        Deal dealSemPayment = Deal.builder()
                .id(UUID.randomUUID())
                .closedBy(sellerId)
                .finalValue(new BigDecimal("2000.00"))
                .paymentMethod(null)
                .archived(false)
                .build();

        when(dealRepository.findByClosedByAndClosedAtBetween(eq(sellerId), any(), any()))
                .thenReturn(List.of(dealComPayment, dealSemPayment));
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any())).thenReturn(Optional.empty());

        UserPerformanceResultDTO result = analyticsService.getUserPerformance(sellerId, period);

        // CASH (1.00): 4000 * 1.00 = 4000.00; deal sem paymentMethod excluído
        assertEquals(new BigDecimal("4000.00"), result.expectedCash());
    }

    @Test
    @DisplayName("getUserPerformance — range cruzando meses lança IllegalArgumentException")
    void getUserPerformance_crossMonthRange_throws() {
        UUID requesterId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        User requester = buildUser(requesterId, null, Role.ADM_SYSTEM);
        User seller = buildUser(sellerId, Sector.COMMERCIAL, Role.USER_COMMERCIAL);

        when(securityUtils.getCurrentUser()).thenReturn(requester);
        when(permissionService.getScope(requester, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        DataRangeDTO crossMonth = new DataRangeDTO(LocalDate.of(2026, 5, 25), LocalDate.of(2026, 6, 5));

        assertThrows(IllegalArgumentException.class,
                () -> analyticsService.getUserPerformance(sellerId, crossMonth));
    }

    @Test
    @DisplayName("getUserPerformance — expõe bonusPeriodRef do mês consultado")
    void getUserPerformance_exposesBonusPeriodRef() {
        UUID requesterId = UUID.randomUUID();
        UUID attendantId = UUID.randomUUID();
        User requester = buildUser(requesterId, null, Role.ADM_SYSTEM);
        User attendant = buildUser(attendantId, Sector.ATTENDANT, Role.USER_ATTENDANT);

        when(securityUtils.getCurrentUser()).thenReturn(requester);
        when(permissionService.getScope(requester, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(userRepository.findById(attendantId)).thenReturn(Optional.of(attendant));
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(bonusConfigRepository.findByRoleAndSectorAndPeriodRef(any(), any(), any())).thenReturn(Optional.empty());

        UserPerformanceResultDTO result = analyticsService.getUserPerformance(attendantId, period);

        assertEquals("2026-05", result.bonusPeriodRef());
    }

    // -------------------------------------------------------------------------
    // getGlobalDashBoard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getGlobalDashBoard — GLOBAL: retorna dashboard com postProcedures e totalExpectedCash corretos")
    void getGlobalDashBoard_global_returnsDashboardWithPostProceduresAndExpectedCash() {
        User user = buildUser(UUID.randomUUID(), null, Role.ADM_SYSTEM);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));

        when(customerRepository.findByAdsChannel(any())).thenReturn(List.of());
        when(leadTicketRepository.findByCustomerIdInAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(dealRepository.findByTicketIdIn(any())).thenReturn(List.of());
        when(configService.sumInvestmentByChannelAndPeriod(any(), any())).thenReturn(BigDecimal.ZERO);
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(userRepository.findByActiveTrue()).thenReturn(List.of());
        when(leadTicketRepository.findByProcedurePerformedAtBetween(any(), any())).thenReturn(List.of());

        // INSTALLMENT: 10000 * 0.85 = 8500.00 | DENTAL_INSURANCE: 5000 * 0.90 = 4500.00 → total: 13000.00
        Deal d1 = buildClosedDeal(UUID.randomUUID(), new BigDecimal("10000.00"), PaymentMethod.INSTALLMENT);
        Deal d2 = buildClosedDeal(UUID.randomUUID(), new BigDecimal("5000.00"), PaymentMethod.DENTAL_INSURANCE);
        when(dealRepository.findByClosedAtBetweenAndArchivedFalse(any(), any())).thenReturn(List.of(d1, d2));

        GlobalDashBoardResultDTO result = analyticsService.getGlobalDashBoard(period);

        assertNotNull(result.postProcedures());
        assertEquals(new BigDecimal("13000.00"), result.totalExpectedCash());
    }

    @Test
    @DisplayName("getGlobalDashBoard — sem deals: totalExpectedCash zero e postProcedures não nulo")
    void getGlobalDashBoard_noDeals_totalExpectedCashIsZeroAndPostProceduresNotNull() {
        User user = buildUser(UUID.randomUUID(), null, Role.ADM_SYSTEM);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));

        when(customerRepository.findByAdsChannel(any())).thenReturn(List.of());
        when(leadTicketRepository.findByCustomerIdInAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(dealRepository.findByTicketIdIn(any())).thenReturn(List.of());
        when(configService.sumInvestmentByChannelAndPeriod(any(), any())).thenReturn(BigDecimal.ZERO);
        when(leadTicketRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(userRepository.findByActiveTrue()).thenReturn(List.of());
        when(dealRepository.findByClosedAtBetweenAndArchivedFalse(any(), any())).thenReturn(List.of());
        when(leadTicketRepository.findByProcedurePerformedAtBetween(any(), any())).thenReturn(List.of());

        GlobalDashBoardResultDTO result = analyticsService.getGlobalDashBoard(period);

        assertEquals(new BigDecimal("0.00"), result.totalExpectedCash());
        assertNotNull(result.postProcedures());
    }

    @Test
    @DisplayName("getGlobalDashBoard — escopo não-GLOBAL: lança AccessDeniedException")
    void getGlobalDashBoard_nonGlobalScope_throwsAccessDenied() {
        User user = buildUser(UUID.randomUUID(), Sector.LEADS, Role.ADM_LEADS);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, ANALYTICS, READ)).thenReturn(Optional.of(PermissionScope.SECTOR));

        assertThrows(AccessDeniedException.class,
                () -> analyticsService.getGlobalDashBoard(period));
    }
}
