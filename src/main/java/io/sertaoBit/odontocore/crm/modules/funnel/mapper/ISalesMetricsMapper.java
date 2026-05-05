package io.sertaoBit.odontocore.crm.modules.funnel.mapper;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.SalesMetricsResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.SalesMetrics;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ISalesMetricsMapper {


    SalesMetricsResponseDTO toResponseDTO(SalesMetrics salesMetrics);

}
