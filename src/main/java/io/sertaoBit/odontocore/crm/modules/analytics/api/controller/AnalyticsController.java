package io.sertaoBit.odontocore.crm.modules.analytics.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.GlobalDashBoardResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.SectorDropOffResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.StageConversionResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.UserPerformanceResultDTO;
import io.sertaoBit.odontocore.crm.modules.analytics.service.AnalyticsService;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService1) {
        this.analyticsService = analyticsService1;

    }


    @GetMapping("/dashboard")
    public ResponseEntity<GlobalDashBoardResultDTO> getGlobalDashBoard(
            @ModelAttribute DataRangeDTO period

    ) {
        return ResponseEntity.ok(
                analyticsService.getGlobalDashBoard(period));
    }


    @GetMapping("/conversion")
    public ResponseEntity<StageConversionResultDTO> getConversionByStage(
            @ModelAttribute DataRangeDTO period,
            @RequestParam @Validated Sector sector
    ) {
        return ResponseEntity.ok(
                analyticsService.getConversionByStage(period, sector)
        );
    }

    @GetMapping("/dropoff")
    public ResponseEntity<List<SectorDropOffResultDTO>> getDropOffBySector(
            @ModelAttribute DataRangeDTO period) {
        return ResponseEntity.ok(
                analyticsService.getDropOffBySector(period));
    }

    @GetMapping("/user-performance/{targetUserId}")
    public ResponseEntity<UserPerformanceResultDTO> getUserPerformance(
            @PathVariable UUID targetUserId,
            @ModelAttribute DataRangeDTO period) {
        return ResponseEntity.ok(
                analyticsService.getUserPerformance(targetUserId, period));
    }

}
