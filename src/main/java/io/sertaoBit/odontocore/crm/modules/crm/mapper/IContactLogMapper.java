package io.sertaoBit.odontocore.crm.modules.crm.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.ContactLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface IContactLogMapper {
    @Mappings({
            @Mapping(target = "contactDate", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "description", source = "description")
    })
    ContactLog toEntity(ContactLogCreateRequestDTO dto);

    @Mapping(target = "description" , source = "description")
    ContactLogResponseDTO toResponseDTO(ContactLog entity);

}
