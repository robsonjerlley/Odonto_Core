package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import lombok.NonNull;

import java.math.BigDecimal;

public record BonusResultDTO(
      @NonNull BigDecimal value
) {
}
