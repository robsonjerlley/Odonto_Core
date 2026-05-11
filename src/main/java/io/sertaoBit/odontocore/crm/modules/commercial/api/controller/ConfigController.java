package io.sertaoBit.odontocore.crm.modules.commercial.api.controller;

import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig.BonusConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.recycleConfig.RecycleConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.service.ConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
