package io.sertaoBit.odontocore.crm.modules.funnel.mapper;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface IDealMapper {
    @Mappings({

            @Mapping(target = "closedDate", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "targetDate", ignore = true),
    })
    Deal toEntity(DealCreateRequestDTO dto);

    DealResponseDTO toResponseDTO(Deal entity);

}
