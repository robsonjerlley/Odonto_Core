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
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "assignedTo", ignore = true),
            @Mapping(target = "customerId", ignore = true),
    })
    LeadTicket toEntity(LeadTicketCreateRequestDTO dto);

    @Mapping(target = "updatedAt", ignore = true)
    LeadTicketResponseDTO toResponseDTO(LeadTicket leadTicket);
}
