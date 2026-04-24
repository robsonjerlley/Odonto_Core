package io.sertaoBit.odontocore.crm.modules.appointment.mapper;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.AppointmentCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.AppointmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Procedure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface IAppointmentMapper {
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "procedures", ignore = true)
    })
    Appointment toEntity(AppointmentCreateRequestDTO dto);


    AppointmentResponseDTO toResponseDTO(Appointment appointment);
}
