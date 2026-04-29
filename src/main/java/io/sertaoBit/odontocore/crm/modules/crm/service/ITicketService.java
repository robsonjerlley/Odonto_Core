package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket.TicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket.TicketUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.TicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface ITicketService {

    TicketResponseDTO create(TicketCreateRequestDTO dto);

    TicketResponseDTO update(UUID id, TicketUpdateRequestDTO dto);

    TicketResponseDTO findById(UUID id);

    List<TicketResponseDTO> findAll();

   List<TicketResponseDTO> findByCustomer(UUID customerId);

   List<TicketResponseDTO> findByTicketStatus(TicketStatus ticketStatus);

    List<TicketResponseDTO> findByAssignedTo(UUID userId);

    TicketResponseDTO updateStatus(UUID id, TicketStatus ticketStatus);

    void deleteById(UUID id);
}
