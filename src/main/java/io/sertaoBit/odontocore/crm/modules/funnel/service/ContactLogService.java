package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;


public interface ContactLogService {

    ContactLogResponseDTO create(ContactLogCreateRequestDTO dto);

    ContactLogResponseDTO findById(UUID id);

    Page<ContactLogResponseDTO> search(UUID ticketId, Pageable pageable);


}
