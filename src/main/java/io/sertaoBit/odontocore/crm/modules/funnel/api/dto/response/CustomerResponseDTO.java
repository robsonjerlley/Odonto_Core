package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.CustomerSource;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponseDTO(
        UUID id,
        String name,
        String cpf,
        String phone,
        String phone2,
        String email,
        String initialNote,
        CustomerSource source,
        AdsChannel adsChannel,
        String adCampaign,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UUID createdBy,
        UUID referredBy,
        boolean anonymized

) {
}
