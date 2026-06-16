package io.sertaoBit.odontocore.crm.modules.funnel.mapper;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ContactLogMapper {
    @Mappings({

            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "statusAfter", ignore = true),
            @Mapping(target = "statusBefore", ignore = true),
            @Mapping(target = "userId", ignore = true),
            @Mapping(target = "username", ignore = true)
    })
    ContactLog toEntity(ContactLogCreateRequestDTO dto);


    ContactLogResponseDTO toResponseDTO(ContactLog entity);




}
