package io.sertaoBit.odontocore.crm.modules.analytics.api.controller;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.analytics.api.dto.*;
import io.sertaoBit.odontocore.crm.modules.analytics.service.AnalyticsService;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;

    public AnalyticsController(AnalyticsService analyticsService1, SecurityUtils securityUtils) {
        this.analyticsService = analyticsService1;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/ads-roi")
    public ResponseEntity<AdsRoiResultDTO> getAdsRoi(
            @RequestParam AdsChannel channel,
            @ModelAttribute DataRangeDTO period
    ) {
        return ResponseEntity.ok(
                analyticsService.getAdsRoi(channel, period, securityUtils.getCurrentUserId())
        );
    }

    @GetMapping("/conversion")
    public ResponseEntity<StageConversionResultDTO> getConversionByStage(
            @ModelAttribute DataRangeDTO period,
            @RequestParam @Validated Sector sector
    ) {
        return ResponseEntity.ok(
                analyticsService.getConversionByStage(period, sector, securityUtils.getCurrentUserId())
        );
    }

    @GetMapping("/dropoff")
    public ResponseEntity<List<SectorDropOffResultDTO>> getDropOffBySector(
            @ModelAttribute DataRangeDTO period) {
        return ResponseEntity.ok(
                analyticsService.getDropOffBySector(period, securityUtils.getCurrentUserId()));
    }

    @GetMapping("/user-performance/{targetUserId}")
    public ResponseEntity<UserPerformanceResultDTO> getUserPerformance(
            @PathVariable UUID targetUserId,
            @ModelAttribute DataRangeDTO period) {
        return ResponseEntity.ok(
                analyticsService.getUserPerformance(targetUserId, period, securityUtils.getCurrentUserId()));
    }

    @GetMapping("/bonus/{id}")
    public ResponseEntity<BonusResultDTO> getCalculatedBonus(
            @PathVariable UUID id,
            @RequestParam String periodRef) {
        return ResponseEntity.ok(
                analyticsService.getCalculatedBonus(id, periodRef, securityUtils.getCurrentUserId()));
    }

    @GetMapping("/post-procedure")
    public ResponseEntity<PostProcedureResultDTO> getPostProcedure(
            @ModelAttribute DataRangeDTO period
    ) {
        return ResponseEntity.ok(analyticsService.getPostProcedureMetrics(period, securityUtils.getCurrentUserId()));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<GlobalDashBoardResultDTO> getGlobalDashBoard(
            @ModelAttribute DataRangeDTO period) {
        return ResponseEntity.ok(
                analyticsService.getGlobalDashBoard(period, securityUtils.getCurrentUserId()));
    }


}
