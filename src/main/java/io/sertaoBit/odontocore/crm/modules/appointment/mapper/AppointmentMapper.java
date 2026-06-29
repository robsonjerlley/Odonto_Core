package io.sertaoBit.odontocore.crm.modules.appointment.mapper;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.AppointmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    AppointmentResponseDTO toResponseDTO(Appointment appointment);


}
