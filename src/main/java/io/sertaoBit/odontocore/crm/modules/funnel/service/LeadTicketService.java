package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.shared.DataRangeDTO;

import java.util.List;
import java.util.UUID;


public interface LeadTicketService {

    LeadTicketResponseDTO create(LeadTicketCreateRequestDTO dto);

    LeadTicketResponseDTO changeStatus(UUID id, TicketStatus status);

    LeadTicketResponseDTO findById(UUID id);

    List<LeadTicketResponseDTO> findAll();

    List<LeadTicketResponseDTO> findByCustomer(UUID customerId);

    List<LeadTicketResponseDTO> findByStatus(TicketStatus status);

    List<LeadTicketResponseDTO> findByAssignedToUser(UUID userId);

    List<LeadTicketResponseDTO> findByPeriod(DataRangeDTO period);

    void deleteById(UUID id);
}

