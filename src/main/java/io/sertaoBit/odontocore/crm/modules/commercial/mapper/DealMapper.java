package io.sertaoBit.odontocore.crm.modules.commercial.mapper;

import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.DealHistoryResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.model.DealHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface DealMapper {
    @Mappings({

            @org.mapstruct.Mapping(target = "archived", ignore = true),
            @org.mapstruct.Mapping(target = "closedAt", ignore = true),
            @org.mapstruct.Mapping(target = "closedBy", ignore = true),
            @org.mapstruct.Mapping(target = "createdAt", ignore = true),
            @org.mapstruct.Mapping(target = "createdBy", ignore = true),
            @org.mapstruct.Mapping(target = "createdBySector", ignore = true),
            @org.mapstruct.Mapping(target = "discountApprovedBy", ignore = true),
            @org.mapstruct.Mapping(target = "discountPct", ignore = true),
            @org.mapstruct.Mapping(target = "finalValue", ignore = true),
            @org.mapstruct.Mapping(target = "id", ignore = true),
            @org.mapstruct.Mapping(target = "paymentMethod", ignore = true),
            @org.mapstruct.Mapping(target = "ticketId", ignore = true),
            @org.mapstruct.Mapping(target = "totalValue", ignore = true),
            @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    })
    Deal toEntity(DealCreateRequestDTO dto);

    DealResponseDTO toResponseDTO(Deal entity);

    DealHistoryResponseDTO toResponseDTO(DealHistory entity);

}
