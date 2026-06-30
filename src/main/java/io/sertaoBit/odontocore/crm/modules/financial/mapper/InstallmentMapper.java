package io.sertaoBit.odontocore.crm.modules.financial.mapper;

import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.InstallmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InstallmentMapper {


    @Mapping(target = "overdue", ignore = true)
    InstallmentResponseDTO toResponseDTO(Installment installment);
}
