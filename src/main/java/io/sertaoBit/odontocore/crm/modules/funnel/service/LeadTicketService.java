package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketChangeStatusRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;


public interface LeadTicketService {

    LeadTicketResponseDTO create(LeadTicketCreateRequestDTO dto);

    LeadTicketResponseDTO changeStatus(UUID id, LeadTicketChangeStatusRequestDTO dto);

    LeadTicketResponseDTO findById(UUID id);

    Page<LeadTicketResponseDTO> search(UUID customerId, TicketStatus status, UUID userId, Pageable pageable);

    void deleteById(UUID id);
}

