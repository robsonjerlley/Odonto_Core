package io.sertaoBit.odontocore.crm.modules.commercial.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig.BonusConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig.RecycleConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.adsInvestment.AdsInvestmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.bonusConfig.BonusConfigResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.recycleConfig.RecycleConfigResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.service.ConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final ConfigService configService;


    public ConfigController(ConfigService configService) {

        this.configService = configService;
    }

    @PostMapping("/recycle")
    public ResponseEntity<Void> setRecycleConfig(
            @RequestBody @Validated RecycleConfigRequestDTO dto
    ) {
        configService.setRecycleConfig(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/bonus")
    public ResponseEntity<Void> setBonusConfig(
            @RequestBody @Validated BonusConfigRequestDTO dto
    ) {
        configService.setBonusConfig(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/ads-investment")
    public ResponseEntity<Void> registerAdsInvestment(
            @RequestBody @Validated AdsInvestmentRequestDTO dto
    ) {
        configService.registerAdsInvestment(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @GetMapping("/bonus")
    public ResponseEntity<List<BonusConfigResponseDTO>> bonus(@RequestParam Sector sector) {
        return ResponseEntity.ok(configService.getBonusConfigs(sector));
    }

    @GetMapping("/ads-investment")
    public ResponseEntity<List<AdsInvestmentResponseDTO>> adsInvestment(@RequestParam AdsChannel channel) {
        return ResponseEntity.ok(configService.getAdsInvestments(channel));
    }

    @GetMapping("/recycle")
    public ResponseEntity<RecycleConfigResponseDTO> getRecycle() {
        return ResponseEntity.ok(configService.getRecycle().orElse(null));
    }

}

