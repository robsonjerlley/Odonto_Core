package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog;

import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ContactLogCreateRequestDTO(
        @NotNull ContactChannel contactChannel,
        @NotBlank String description,
        @NotNull LocalDateTime occurredAt

) {
}
