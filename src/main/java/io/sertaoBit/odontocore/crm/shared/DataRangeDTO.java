package io.sertaoBit.odontocore.crm.shared;

import java.time.LocalDate;

public record DataRangeDTO(
        LocalDate from,
        LocalDate to
) {
}
