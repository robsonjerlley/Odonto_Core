package io.sertaoBit.odontocore.crm.modules.crm.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.ContactLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IContactLogMapper {
    @Mappings({
            @Mapping(target = "contactDate", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "description", source = "description", qualifiedByName = "stringToList")
    })
    ContactLog toEntity(ContactLogCreateRequestDTO dto);


    ContactLogResponseDTO toResponseDTO(ContactLog entity);

    @Named("stringToList")
    default List<String> stringToList(String description) {
        return description != null && !description.isBlank()
                ? List.of(description)
                : List.of();
    }
}
