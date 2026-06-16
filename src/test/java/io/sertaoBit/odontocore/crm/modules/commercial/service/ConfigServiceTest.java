package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig.RecycleConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.adsInvestment.AdsInvestmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.bonusConfig.BonusConfigResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.recycleConfig.RecycleConfigResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.mapper.AdsInvestmentMapper;
import io.sertaoBit.odontocore.crm.modules.commercial.mapper.BonusConfigMapper;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
import io.sertaoBit.odontocore.crm.modules.commercial.model.BonusConfig;
import io.sertaoBit.odontocore.crm.modules.commercial.model.RecycleConfig;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.AdsInvestmentRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.BonusConfigRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.RecycleConfigRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.service.impl.ConfigServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigService - Testes Unitários")
public class ConfigServiceTest {

    private ConfigService configService;
    @Mock private RecycleConfigRepository configRepository;
    @Mock private BonusConfigRepository bonusRepository;
    @Mock private AdsInvestmentRepository adsInvestmentRepository;
    @Mock private BonusConfigMapper bonusConfigMapper;
    @Mock private AdsInvestmentMapper adsInvestmentMapper;
    @Mock private PermissionService permissionService;
    @Mock private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        configService = new ConfigServiceImpl(
                configRepository, bonusRepository,
                adsInvestmentRepository, bonusConfigMapper, adsInvestmentMapper,
                permissionService, securityUtils
        );
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Manager")
                .username("manager@test.com")
                .password("hash")
                .sector(Sector.LEADS)
                .role(Role.ADM_LEADS)
                .active(true)
                .build();
    }

    // ========== SET RECYCLE CONFIG ==========

    @Test
    @DisplayName("Deve salvar nova config de reciclo global com sucesso")
    void setRecycleConfig_success() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecycleConfig result = configService.setRecycleConfig(new RecycleConfigRequestDTO(15));

        assertEquals(15, result.getAfterDays());
        verify(configRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException ao configurar reciclo sem permissão")
    void setRecycleConfig_accessDenied() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).checkOrThrow(any(), any(), any(), any(), any());

        assertThrows(AccessDeniedException.class,
                () -> configService.setRecycleConfig(new RecycleConfigRequestDTO(15)));

        verify(configRepository, never()).save(any());
    }

    // ========== REGISTER ADS INVESTMENT ==========

    @Test
    @DisplayName("Deve registrar investimento em ADS com sucesso")
    void registerAdsInvestment_success() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());

        AdsInvestmentRequestDTO dto = new AdsInvestmentRequestDTO(
                AdsChannel.META, "campanha-1",
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        when(adsInvestmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdsInvestment result = configService.registerAdsInvestment(dto);

        assertNotNull(result);
        assertEquals(AdsChannel.META, result.getChannel());
        assertEquals(new BigDecimal("5000.00"), result.getAmount());
        verify(adsInvestmentRepository).save(any());
    }

    // ========== GET RECYCLE (ADR-007) ==========

    @Test
    @DisplayName("Deve retornar a config de reciclo ativa mais recente")
    void getRecycle_success() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());

        RecycleConfig config = RecycleConfig.builder()
                .id(UUID.randomUUID())
                .afterDays(30)
                .active(true)
                .configuredBy(UUID.randomUUID())
                .build();

        when(configRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(config));

        Optional<RecycleConfigResponseDTO> result = configService.getRecycle();

        assertTrue(result.isPresent());
        assertEquals(30, result.get().afterDays());
        assertTrue(result.get().active());
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando nenhuma config de reciclo está cadastrada — bug #18")
    void getRecycle_semConfig_retornaVazio() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(configRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        Optional<RecycleConfigResponseDTO> result = configService.getRecycle();

        assertTrue(result.isEmpty());
        verify(configRepository).findFirstByActiveTrueOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException ao consultar reciclo sem permissão")
    void getRecycle_accessDenied() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).checkOrThrow(any(), any(), any(), any(), any());

        assertThrows(AccessDeniedException.class, () -> configService.getRecycle());

        verify(configRepository, never()).findFirstByActiveTrueOrderByCreatedAtDesc();
    }

    // ========== GET BONUS CONFIGS (ADR-007) ==========

    @Test
    @DisplayName("Deve retornar lista de BonusConfig filtrada por setor")
    void getBonusConfigs_success() {
        User user = buildUser();
        when(securityUtils.getCurrentUser()).thenReturn(user);

        BonusConfig bonus = BonusConfig.builder()
                .id(UUID.randomUUID())
                .sector(Sector.COMMERCIAL)
                .role(Role.USER_COMMERCIAL)
                .metricKey("deals_closed")
                .bonusPct(new BigDecimal("5.00"))
                .targetValue(new BigDecimal("10"))
                .periodRef("2026-05")
                .active(true)
                .build();

        BonusConfigResponseDTO dto = new BonusConfigResponseDTO(
                bonus.getId(), bonus.getSector(), bonus.getRole(),
                bonus.getMetricKey(), bonus.getBonusPct(), bonus.getTargetValue(),
                bonus.getPeriodRef(), bonus.isActive(), null
        );

        when(bonusRepository.findBySector(Sector.COMMERCIAL)).thenReturn(List.of(bonus));
        when(bonusConfigMapper.toResponseDTO(bonus)).thenReturn(dto);

        List<BonusConfigResponseDTO> result = configService.getBonusConfigs(Sector.COMMERCIAL);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Sector.COMMERCIAL, result.get(0).sector());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há BonusConfig para o setor")
    void getBonusConfigs_empty() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(bonusRepository.findBySector(Sector.COMMERCIAL)).thenReturn(List.of());

        List<BonusConfigResponseDTO> result = configService.getBonusConfigs(Sector.COMMERCIAL);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== GET ADS INVESTMENTS (ADR-007) ==========

    @Test
    @DisplayName("Deve retornar lista de AdsInvestment filtrada por canal")
    void getAdsInvestments_success() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());

        AdsInvestment investment = AdsInvestment.builder()
                .id(UUID.randomUUID())
                .channel(AdsChannel.META)
                .campaign("campanha-1")
                .amount(new BigDecimal("3000.00"))
                .periodStart(LocalDate.of(2026, 5, 1))
                .periodEnd(LocalDate.of(2026, 5, 31))
                .build();

        AdsInvestmentResponseDTO dto = new AdsInvestmentResponseDTO(
                investment.getId(), investment.getChannel(), investment.getCampaign(),
                investment.getAmount(), investment.getPeriodStart(), investment.getPeriodEnd(), null
        );

        when(adsInvestmentRepository.findByChannelOrderByPeriodStartDesc(AdsChannel.META))
                .thenReturn(List.of(investment));
        when(adsInvestmentMapper.toResponseDTO(investment)).thenReturn(dto);

        List<AdsInvestmentResponseDTO> result = configService.getAdsInvestments(AdsChannel.META);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(AdsChannel.META, result.get(0).channel());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há investimentos para o canal")
    void getAdsInvestments_empty() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(adsInvestmentRepository.findByChannelOrderByPeriodStartDesc(AdsChannel.GOOGLE))
                .thenReturn(List.of());

        List<AdsInvestmentResponseDTO> result = configService.getAdsInvestments(AdsChannel.GOOGLE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
