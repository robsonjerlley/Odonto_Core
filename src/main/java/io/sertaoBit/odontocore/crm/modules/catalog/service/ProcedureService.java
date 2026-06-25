package io.sertaoBit.odontocore.crm.modules.catalog.service;


import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.request.ProcedureUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProcedureService {

    ProcedureResponseDTO create(ProcedureCreateRequestDTO dto);

    ProcedureResponseDTO update(UUID procedureId, ProcedureUpdateRequestDTO dto);

    Page<ProcedureResponseDTO> search(String name, String code, Pageable pageable);

    void softDelete(UUID procedureId);
}
