package io.sertaoBit.odontocore.crm.modules.funnel.mapper;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface IContactLogMapper {
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "customer" , ignore = true),
            @Mapping(target = "ticket" ,ignore = true ),
            @Mapping(target = "contactBy", ignore = true),
            @Mapping(target = "description", source = "description"),
            @Mapping(target = "contactDate", ignore = true),
    })
    ContactLog toEntity(ContactLogCreateRequestDTO dto);

    @Mapping(target = "description" , source = "description")
    ContactLogResponseDTO toResponseDTO(ContactLog entity);

}
