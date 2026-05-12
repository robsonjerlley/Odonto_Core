package io.sertaoBit.odontocore.crm.modules.analytics.api.dto;

import java.time.LocalDate;

public record DataRangeDTO(
        LocalDate from,
        LocalDate to
) {
}
