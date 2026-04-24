package io.sertaoBit.odontocore.crm.modules.appointment.service;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.ProcedureUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.ProcedureResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IProcedureService {

    ProcedureResponseDTO create(ProcedureCreateRequestDTO dto);

    ProcedureResponseDTO update(UUID id, ProcedureUpdateRequestDTO dto);

    ProcedureResponseDTO findById(UUID id);

    List<ProcedureResponseDTO> findByName(String name);

    void delete(UUID id);
}
