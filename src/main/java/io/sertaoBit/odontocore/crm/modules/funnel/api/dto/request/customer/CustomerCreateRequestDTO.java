package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer;


import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.CustomerSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

public record CustomerCreateRequestDTO(
        @NotBlank String name,
        @CPF String cpf,
        @NotBlank String telephone,
        @NotBlank String city,
        @NotBlank String address,
        @NotBlank String description,
        @NotNull CustomerSource source,
        @NotNull AdsChannel adsChannel,
        @NotBlank String adCampaign

) {
}
