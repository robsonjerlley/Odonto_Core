package io.sertaoBit.odontocore.crm.modules.analytics.service;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.AdsRoiResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.StageConversionResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.service.impl.AnalyticsServiceImpl;
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
        when(customerRepository.findByAdChannel(AdsChannel.META)).thenReturn(List.of());
        when(leadTicketRepository.findByCustomerIdInAndStatusAndClosedAtBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(dealRepository.findByTicketIdIn(any())).thenReturn(List.of());
        when(configService.sumInvestmentByChannelAndPeriod(any(), any())).thenReturn(BigDecimal.ZERO);

        AdsRoiResultDTO result = analyticsService.getAdsRoi(AdsChannel.META, period, userId);

        assertEquals(BigDecimal.ZERO, result.roiMultiplier());
        assertEquals(BigDecimal.ZERO, result.totalRevenue());
    }

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

        BigDecimal result = analyticsService.getCalculatedBonus(targetId, "2026-05", userId);

        assertEquals(BigDecimal.ZERO, result);
    }

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
}
