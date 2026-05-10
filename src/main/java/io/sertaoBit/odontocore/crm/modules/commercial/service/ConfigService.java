package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig.BonusConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig.RecycleConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
import io.sertaoBit.odontocore.crm.modules.commercial.model.BonusConfig;
import io.sertaoBit.odontocore.crm.modules.commercial.model.RecycleConfig;

import java.nio.file.AccessDeniedException;

public interface ConfigService {

    RecycleConfig setRecycleConfig(RecycleConfigRequestDTO dto) throws AccessDeniedException;

    BonusConfig setBonusConfig(BonusConfigRequestDTO dto);

    AdsInvestment registerAdsInvestment(AdsInvestmentRequestDTO dto);
}
