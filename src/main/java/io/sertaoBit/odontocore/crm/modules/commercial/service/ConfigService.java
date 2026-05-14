package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig.BonusConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig.RecycleConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
import io.sertaoBit.odontocore.crm.modules.commercial.model.BonusConfig;
import io.sertaoBit.odontocore.crm.modules.commercial.model.RecycleConfig;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;

import java.math.BigDecimal;

public interface ConfigService {

    RecycleConfig setRecycleConfig(RecycleConfigRequestDTO dto);

    BonusConfig setBonusConfig(BonusConfigRequestDTO dto);

    AdsInvestment registerAdsInvestment(AdsInvestmentRequestDTO dto);

    BigDecimal sumInvestmentByChannelAndPeriod(AdsChannel channel, DataRangeDTO period);
}
