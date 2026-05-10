package io.sertaoBit.odontocore.crm.modules.commercial.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Action;
import io.sertaoBit.odontocore.crm.core.enums.Resource;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig.BonusConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig.RecycleConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
import io.sertaoBit.odontocore.crm.modules.commercial.model.BonusConfig;
import io.sertaoBit.odontocore.crm.modules.commercial.model.RecycleConfig;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.AdsInvestmentRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.BonusConfigRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.RecycleConfigRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.service.ConfigService;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final RecycleConfigRepository configRepository;
    private final BonusConfigRepository bonusRepository;
    private final AdsInvestmentRepository adsInvestmentRepository;
    private final PermissionService permissionService;
    private final SecurityUtils securityUtils;

    public ConfigServiceImpl(RecycleConfigRepository configRepository, BonusConfigRepository bonusRepository, AdsInvestmentRepository adsInvestmentRepository, PermissionService permissionService, SecurityUtils securityUtils) {
        this.configRepository = configRepository;
        this.bonusRepository = bonusRepository;
        this.adsInvestmentRepository = adsInvestmentRepository;
        this.permissionService = permissionService;
        this.securityUtils = securityUtils;
    }


    @Override
    @Transactional
    public RecycleConfig setRecycleConfig(RecycleConfigRequestDTO dto) {
        var currentUser = securityUtils.getCurrentUser();
        if (!permissionService.canAccess(
                currentUser, Resource.CONFIG,
                Action.CONFIGURE, dto.sector(),
                null)) {
            throw new AccessDeniedException("User not have access");
        }

        RecycleConfig recycleConfig = RecycleConfig.builder()
                .sector(dto.sector())
                .afterDays(dto.afterDays())
                .configuredBy(currentUser.getId())
                .build();

        return configRepository.save(recycleConfig);
    }

    @Override
    @Transactional
    public BonusConfig setBonusConfig(BonusConfigRequestDTO dto) {
        var currentUser = securityUtils.getCurrentUser();
        if (!permissionService.canAccess(
                currentUser, Resource.CONFIG,
                Action.CONFIGURE, dto.sector(),
                null)) {
            throw new AccessDeniedException("User not have access");
        }

        BonusConfig bonusConfig = BonusConfig.builder()
                .sector(dto.sector())
                .role(dto.role())
                .metricKey(dto.metricKey())
                .bonusPct(dto.bonusPct())
                .targetValue(dto.targetValue())
                .periodRef(dto.periodRef())
                .configuredBy(currentUser.getId())
                .build();

        return bonusRepository.save(bonusConfig);
    }

    @Override
    @Transactional
    public AdsInvestment registerAdsInvestment(AdsInvestmentRequestDTO dto) {
        var  currentUser = securityUtils.getCurrentUser();
        if (!permissionService.canAccess(
                currentUser, Resource.CONFIG,
                Action.CONFIGURE, null,
                null)) {
            throw new AccessDeniedException("User not have access");
        }

        AdsInvestment adsInvestment = AdsInvestment.builder()
                .channel(dto.channel())
                .campaign(dto.campaign())
                .amount(dto.amount())
                .periodStart(dto.periodStart())
                .periodEnd(dto.periodEnd())
                .registeredBy(currentUser.getId())
                .build();

        return adsInvestmentRepository.save(adsInvestment);
    }
}
