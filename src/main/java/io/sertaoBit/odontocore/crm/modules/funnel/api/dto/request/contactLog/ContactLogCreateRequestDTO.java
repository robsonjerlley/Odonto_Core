package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog;

import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;


public record ContactLogCreateRequestDTO(
        @NotNull UUID ticketId,
        @NotNull ContactChannel channel,
        @NotBlank String note,
        @NotNull LocalDateTime occurredAt

) {
}
