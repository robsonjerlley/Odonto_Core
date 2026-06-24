package io.sertaoBit.odontocore.crm.modules.catalog.service;


import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ProcedureService {

    ProcedureResponseDTO create(ProcedureCreateRequestDTO dto);

    ProcedureResponseDTO update(UUID procedureId, ProcedureUpdateRequestDTO dto);

    ProcedureResponseDTO findByName(String name);

    ProcedureResponseDTO isActive(List<UUID> procedureId);

    void delete(UUID procedureId);
}
