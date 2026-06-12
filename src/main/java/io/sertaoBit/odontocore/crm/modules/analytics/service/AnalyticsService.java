package io.sertaoBit.odontocore.crm.modules.analytics.service;

import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.GlobalDashBoardResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.SectorDropOffResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.StageConversionResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.UserPerformanceResultDTO;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;

import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

    StageConversionResultDTO getConversionByStage(DataRangeDTO period, Sector sector);

    List<SectorDropOffResultDTO> getDropOffBySector(DataRangeDTO period);

    UserPerformanceResultDTO getUserPerformance(UUID targetUserId, DataRangeDTO period);

    GlobalDashBoardResultDTO getGlobalDashBoard(DataRangeDTO period);

}
