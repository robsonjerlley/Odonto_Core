package io.sertaoBit.odontocore.crm.modules.appointment.service;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.AppointmentCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.AppointmentUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.AppointmentResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IAppointmentService {

    AppointmentResponseDTO create(AppointmentCreateRequestDTO dto);

    AppointmentResponseDTO update(UUID id, AppointmentUpdateRequestDTO dto);

    AppointmentResponseDTO findById(UUID id);

    List<AppointmentResponseDTO> findAll();

    void deleteById(UUID id);

}
