package io.sertaoBit.odontocore.crm.modules.catalog.mapper;

import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureView;
import io.sertaoBit.odontocore.crm.modules.catalog.domain.model.Procedure;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProcedureMapper {

    ProcedureResponseDTO toResponseDTO(Procedure procedure);


    ProcedureView toView(Procedure procedure);
}
