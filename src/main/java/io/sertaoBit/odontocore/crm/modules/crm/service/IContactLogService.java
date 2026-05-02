package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactOutcome;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public interface IContactLogService {

    ContactLogResponseDTO create(ContactLogCreateRequestDTO dto);

    ContactLogResponseDTO update(UUID id, ContactLogUpdateRequestDTO dto);

    ContactLogResponseDTO findById(UUID id);

    List<ContactLogResponseDTO> findAll();

    List<ContactLogResponseDTO> findByCustomer(UUID id);

    List<ContactLogResponseDTO> findByContactByUser(UUID id);

    List<ContactLogResponseDTO> findByChannel(ContactChannel channel);

    List<ContactLogResponseDTO> findOutcome(ContactOutcome contactOutcome);

    List<ContactLogResponseDTO> findByDateRange(LocalDate start, LocalDate end);

    List<ContactLogResponseDTO> findWithPendingFollowUp();

    void delete(UUID id);


}
