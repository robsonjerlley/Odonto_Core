package io.sertaoBit.odontocore.crm.modules.crm.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.SalesMetricsResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.SalesMetrics;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ISalesMetricsMapper {


    SalesMetricsResponseDTO toResponseDTO(SalesMetrics salesMetrics);

}
