package io.sertaoBit.odontocore.crm.modules.funnel.mapper;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface LeadTicketMapper {

    @Mappings({

            @Mapping(target = "closedAt", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "pendingAt", ignore = true),
            @Mapping(target = "previousTicketId", ignore = true),
            @Mapping(target = "recycledAt", ignore = true),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "procedurePerformedAt", ignore = true),
            @Mapping(target = "returnScheduledAt", ignore = true),
            @Mapping(target = "clinicId", ignore = true)
    })
    LeadTicket toEntity(LeadTicketCreateRequestDTO dto);


    LeadTicketResponseDTO toResponseDTO(LeadTicket leadTicket);
}
