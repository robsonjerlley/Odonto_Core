package io.sertaoBit.odontocore.crm.modules.appointment.mapper;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Procedure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IProcedureMapper {

    @Mapping(target = "id", ignore = true)
    Procedure toEntity(ProcedureCreateRequestDTO dto);

    ProcedureResponseDTO toResponseDTO(Procedure entity);
}
