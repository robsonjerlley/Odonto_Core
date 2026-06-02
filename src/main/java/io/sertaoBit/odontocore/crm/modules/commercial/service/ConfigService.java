package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig.BonusConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig.RecycleConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.adsInvestment.AdsInvestmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.bonusConfig.BonusConfigResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.recycleConfig.RecycleConfigResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
import io.sertaoBit.odontocore.crm.modules.commercial.model.BonusConfig;
import io.sertaoBit.odontocore.crm.modules.commercial.model.RecycleConfig;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ConfigService {

    RecycleConfig setRecycleConfig(RecycleConfigRequestDTO dto);

    BonusConfig setBonusConfig(BonusConfigRequestDTO dto);

    AdsInvestment registerAdsInvestment(AdsInvestmentRequestDTO dto);

    BigDecimal sumInvestmentByChannelAndPeriod(AdsChannel channel, DataRangeDTO period);

    RecycleConfigResponseDTO getRecycle();

    List<BonusConfigResponseDTO>  getBonusConfigs(Sector sector);

    List<AdsInvestmentResponseDTO>  getAdsInvestments(AdsChannel channel);
}
