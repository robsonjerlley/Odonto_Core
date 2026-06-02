package io.sertaoBit.odontocore.crm.modules.commercial.mapper;

import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.bonusConfig.BonusConfigRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.bonusConfig.BonusConfigResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.BonusConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface BonusConfigMapper {
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "configuredBy", ignore = true),
            @Mapping(target = "active", ignore = true)
    })
    BonusConfig toEntity(BonusConfigRequestDTO dto);

    BonusConfigResponseDTO toResponseDTO(BonusConfig bonusConfig);

}
