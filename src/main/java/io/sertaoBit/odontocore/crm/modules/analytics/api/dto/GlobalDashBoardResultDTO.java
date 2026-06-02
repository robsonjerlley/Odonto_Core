package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;

import java.math.BigDecimal;
import java.util.List;

public record GlobalDashBoardResultDTO(
        DataRangeDTO period,
        List<AdsRoiResultDTO> adsRoi,
        StageConversionResultDTO stageConversion,
        List<SectorDropOffResultDTO> sectorDropOff,
        List<UserPerformanceResultDTO> topPerformers,
        BigDecimal totalExpectedCash
) {
}
