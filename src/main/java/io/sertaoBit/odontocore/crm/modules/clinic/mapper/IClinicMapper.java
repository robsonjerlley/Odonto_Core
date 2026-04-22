package io.sertaoBit.odontocore.crm.modules.clinic.mapper;

import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.response.ClinicResponseDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.domain.model.Clinic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IClinicMapper {

    @Mapping(target = "id",  ignore = true)
    @Mapping(target = "employees" ,  ignore = true)
    Clinic toEntity(ClinicCreateRequestDTO dto);


    ClinicResponseDTO toResponseDTO(Clinic entity);

}
