package io.sertaoBit.odontocore.crm.modules.crm.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket.TicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.TicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ITicketMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "assigneTo", ignore = true),
            @Mapping(target = "customer", ignore = true),
            @Mapping(target = "dueDate", source = "dueDate")
    })
    Ticket toEntity(TicketCreateRequestDTO dto);

    @Mapping(target = "updateAt", ignore = true)
    TicketResponseDTO toResponseDTO(Ticket ticket);
}
