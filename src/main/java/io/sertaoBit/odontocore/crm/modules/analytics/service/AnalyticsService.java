package io.sertaoBit.odontocore.crm.modules.analytics.service;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.AdsRoiResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.DataRangeDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    AdsRoiResultDTO getAdsRoi(
            AdsChannel channel,
            DataRangeDTO period,
            User by
    ){
        return null;
    }


}
