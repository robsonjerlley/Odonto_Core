package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig.RecycleConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
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
    @Mock private PermissionService permissionService;
    @Mock private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        configService = new ConfigServiceImpl(
                configRepository, bonusRepository,
                adsInvestmentRepository, permissionService, securityUtils);
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Manager")
                .username("manager@test.com")
                .passwordHash("hash")
                .sector(Sector.LEADS)
                .role(Role.ADM_LEADS)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve desativar config antiga e salvar nova ao configurar reciclo")
    void setRecycleConfig_success() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());

        RecycleConfig old = RecycleConfig.builder()
                .id(UUID.randomUUID())
                .sector(Sector.LEADS)
                .afterDays(30)
                .active(true)
                .configuredBy(UUID.randomUUID())
                .build();

        when(configRepository.findBySectorAndActiveTrue(Sector.LEADS)).thenReturn(Optional.of(old));
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecycleConfig result = configService.setRecycleConfig(
                new RecycleConfigRequestDTO(Sector.LEADS, 15));

        assertFalse(old.isActive());
        assertEquals(15, result.getAfterDays());
        verify(configRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException ao configurar reciclo sem permissão")
    void setRecycleConfig_accessDenied() {
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).checkOrThrow(any(), any(), any(), any(), any());

        assertThrows(AccessDeniedException.class,
                () -> configService.setRecycleConfig(new RecycleConfigRequestDTO(Sector.LEADS, 15)));

        verify(configRepository, never()).save(any());
    }

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
}
