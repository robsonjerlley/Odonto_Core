package io.sertaoBit.odontocore.crm.modules.commercial.mapper;

import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.adsInvestment.AdsInvestmentRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.adsInvestment.AdsInvestmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface AdsInvestmentMapper {

    @Mappings({
            @Mapping(target = "registeredBy", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true)
    })
    AdsInvestment toEntity(AdsInvestmentRequestDTO dto);

    AdsInvestmentResponseDTO toResponseDTO(AdsInvestment entity);

}
