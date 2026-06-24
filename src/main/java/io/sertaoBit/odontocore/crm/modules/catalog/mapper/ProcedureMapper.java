package io.sertaoBit.odontocore.crm.modules.catalog.mapper;

import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.domain.model.Procedure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ProcedureMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "clinicId", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true)
    })
    Procedure toEntity(ProcedureCreateRequestDTO dto);

    ProcedureResponseDTO toResponseDTO(Procedure procedure);

}
