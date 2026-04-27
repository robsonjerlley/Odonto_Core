package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactOutcome;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public interface IContactLogService {

    ContactLogResponseDTO create(ContactLogCreateRequestDTO dto);

    ContactLogResponseDTO update(UUID id, ContactLogUpdateRequestDTO dto);

    ContactLogResponseDTO findById(UUID id);

    List<ContactLogResponseDTO> findByAll();

    ContactLogResponseDTO findByCustomer(UUID id);

    ContactLogResponseDTO findByContactBy(UUID id);

    ContactLogResponseDTO findByChannelBy(UUID id);

    ContactLogResponseDTO findOutcome(ContactOutcome contactOutcome);

    ContactLogResponseDTO findByDateRange(LocalDateTime start, LocalDateTime end);

    List<ContactLogResponseDTO> findWithPendingFollowUp();

    void delete(UUID id);


}
