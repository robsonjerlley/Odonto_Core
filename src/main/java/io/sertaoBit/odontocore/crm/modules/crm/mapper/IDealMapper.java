package io.sertaoBit.odontocore.crm.modules.crm.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Deal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface IDealMapper {
    @Mappings({
            @Mapping(target = "targetDate", ignore = true),
            @Mapping(target = "closedDate", ignore = true),
            @Mapping(target = "id", ignore = true)
    })
    Deal toEntity(DealCreateRequestDTO dto);

    DealResponseDTO toResponseDTO(Deal entity);

}
