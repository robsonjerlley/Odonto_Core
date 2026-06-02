package io.sertaoBit.odontocore.crm.modules.commercial.mapper;

import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.deal.DealHistoryResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.deal.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.model.DealHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface DealMapper {
    @Mappings({

            @Mapping(target = "archived", ignore = true),
            @Mapping(target = "closedAt", ignore = true),
            @Mapping(target = "closedBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdBySector", ignore = true),
            @Mapping(target = "discountApprovedBy", ignore = true),
            @Mapping(target = "discountPct", ignore = true),
            @Mapping(target = "finalValue", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "paymentMethod", ignore = true),
            @Mapping(target = "ticketId", ignore = true),
            @Mapping(target = "totalValue", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    Deal toEntity(DealCreateRequestDTO dto);

    DealResponseDTO toResponseDTO(Deal entity);

    DealHistoryResponseDTO toResponseDTO(DealHistory entity);

}
