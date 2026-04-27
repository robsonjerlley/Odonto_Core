package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.ContactLogResponseDTO;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface IContactLogService {

    ContactLogResponseDTO create(ContactLogCreateRequestDTO dto);

    ContactLogResponseDTO update(ContactLogUpdateRequestDTO dto);

    ContactLogResponseDTO findById(UUID id);

    ContactLogResponseDTO findByAll();

    void delete(UUID id);


}
