package io.sertaoBit.odontocore.crm.modules.commercial.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig.BonusConfigRequestDTO;
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
import io.sertaoBit.odontocore.crm.modules.commercial.service.ConfigService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static io.sertaoBit.odontocore.crm.core.enums.Action.CONFIGURE;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.CONFIG;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final RecycleConfigRepository configRepository;
    private final BonusConfigRepository bonusRepository;
    private final AdsInvestmentRepository adsInvestmentRepository;
    private final BonusConfigMapper bonusConfigMapper;
    private final AdsInvestmentMapper adsInvestmentMapper;
    private final PermissionService permissionService;
    private final SecurityUtils securityUtils;

    public ConfigServiceImpl(
            RecycleConfigRepository configRepository,
            BonusConfigRepository bonusRepository,
            AdsInvestmentRepository adsInvestmentRepository,
            BonusConfigMapper bonusConfigMapper,
            AdsInvestmentMapper adsInvestmentMapper,
            PermissionService permissionService,
            SecurityUtils securityUtils
    ) {
        this.configRepository = configRepository;
        this.bonusRepository = bonusRepository;
        this.adsInvestmentRepository = adsInvestmentRepository;
        this.bonusConfigMapper = bonusConfigMapper;
        this.adsInvestmentMapper = adsInvestmentMapper;
        this.permissionService = permissionService;
        this.securityUtils = securityUtils;
    }


    @Override
    @Transactional
    public RecycleConfig setRecycleConfig(RecycleConfigRequestDTO dto) {

        var currentUser = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                currentUser, CONFIG,
                CONFIGURE, null,
                null
        );

        RecycleConfig recycleConfig = RecycleConfig.builder()
                .afterDays(dto.afterDays())
                .configuredBy(currentUser.getId())
                .build();

        return configRepository.save(recycleConfig);
    }

    @Override
    @Transactional
    public BonusConfig setBonusConfig(BonusConfigRequestDTO dto) {
        var currentUser = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                currentUser, CONFIG,
                CONFIGURE, dto.sector(),
                null
        );

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
        var currentUser = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                currentUser, CONFIG,
                CONFIGURE, null,
                null
        );

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

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumInvestmentByChannelAndPeriod(AdsChannel channel, DataRangeDTO period) {

        return adsInvestmentRepository.sumAmountByChannelAndPeriod(channel, period.from(), period.to());


    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecycleConfigResponseDTO> getRecycle() {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                CONFIG,
                CONFIGURE,
                null,
                null
        );

        return configRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .map(recycle ->
                        new RecycleConfigResponseDTO(
                                recycle.getId(),
                                recycle.getAfterDays(),
                                recycle.isActive(),
                                recycle.getCreatedAt()
                        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BonusConfigResponseDTO> getBonusConfigs(Sector sector) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                CONFIG,
                CONFIGURE,
                user.getSector(),
                user.getId()
        );


        return bonusRepository.findBySector(sector).stream()
                .map(bonusConfigMapper::toResponseDTO)
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public List<AdsInvestmentResponseDTO> getAdsInvestments(AdsChannel channel) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                CONFIG,
                CONFIGURE,
                null,
                null
        );

        return adsInvestmentRepository.findByChannelOrderByPeriodStartDesc(channel).stream()
                .map(adsInvestmentMapper::toResponseDTO)
                .toList();
    }
}
