package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface LeadTicketService {

    LeadTicketResponseDTO create(LeadTicketCreateRequestDTO dto);

    LeadTicketResponseDTO update(UUID id, LeadTicketUpdateRequestDTO dto);

    LeadTicketResponseDTO findById(UUID id);

    List<LeadTicketResponseDTO> findAll();

    List<LeadTicketResponseDTO> findByCustomer(UUID customerId);

    List<LeadTicketResponseDTO> findByTicketStatus(TicketStatus ticketStatus);

    List<LeadTicketResponseDTO> findByAssignedToUser(UUID userId);

    LeadTicketResponseDTO updateStatus(UUID id, TicketStatus ticketStatus);

    void deleteById(UUID id);
}
