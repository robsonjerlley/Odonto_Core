package io.sertaoBit.odontocore.crm.modules.funnel.mapper;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ContactLogMapper {
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "customerId" , ignore = true),
            @Mapping(target = "leadTicket",ignore = true ),
            @Mapping(target = "contactBy", ignore = true),
            @Mapping(target = "note", source = "note"),
            @Mapping(target = "contactDate", ignore = true),
    })
    ContactLog toEntity(ContactLogCreateRequestDTO dto);

    @Mapping(target = "note" , source = "note")
    ContactLogResponseDTO toResponseDTO(ContactLog entity);

}
