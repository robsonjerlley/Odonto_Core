package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.CustomerSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerResponseDTO(
        @NotNull UUID id,
        @NotBlank String name,
        @CPF String cpf,
        @NotBlank String telephone,
        @NotBlank String city,
        @NotBlank String address,
        @NotBlank List<String> description,
        @NotNull CustomerSource source,
        @NotNull AdsChannel adsChannel,
        @NotBlank String adCampaign,
        @NotNull LocalDateTime createdAt,
        @NotNull LocalDateTime updatedAt,
        @NotNull UUID createdByUser
) {
}
