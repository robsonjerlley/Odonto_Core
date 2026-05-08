package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;

import java.util.List;
import java.util.UUID;


public interface LeadTicketService {

    LeadTicketResponseDTO create(LeadTicketCreateRequestDTO dto);

    LeadTicketResponseDTO changeStatus(UUID id, TicketStatus ticketStatus);

    LeadTicketResponseDTO findById(UUID id);

    List<LeadTicketResponseDTO> findAll();

    List<LeadTicketResponseDTO> findByCustomer(UUID customerId);

    List<LeadTicketResponseDTO> findByStatus(TicketStatus ticketStatus);

    List<LeadTicketResponseDTO> findByAssignedToUser(UUID userId);


    void deleteById(UUID id);
}

