package io.sertaoBit.odontocore.crm.modules.analytics.service;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.*;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;

import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

    AdsRoiResultDTO getAdsRoi(AdsChannel channel, DataRangeDTO period, UUID userId);

    StageConversionResultDTO getConversionByStage(DataRangeDTO period, Sector sector, UUID userId);

    List<SectorDropOffResultDTO> getDropOffBySector(DataRangeDTO period, UUID userId);

    UserPerformanceResultDTO getUserPerformance(UUID targetUserId ,DataRangeDTO period, UUID userId);

    BonusResultDTO getCalculatedBonus(UUID targetId, String periodRef , UUID userId);

    GlobalDashBoardResultDTO getGlobalDashBoard(DataRangeDTO period, UUID userId);

    PostProcedureResultDTO getPostProcedureMetrics(DataRangeDTO period, UUID userId);
}
