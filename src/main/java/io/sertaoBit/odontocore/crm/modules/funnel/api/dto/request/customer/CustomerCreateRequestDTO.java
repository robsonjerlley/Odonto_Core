package io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer;


import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.CustomerSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.util.UUID;

public record CustomerCreateRequestDTO(
        @NotBlank String name,
        @CPF String cpf,
        @NotBlank String phone,
        String phone2,
        String email,
        String initialNote,
        @NotNull CustomerSource source,
        AdsChannel adChannel,
        String adCampaign,
        UUID referredBy

) {
}
