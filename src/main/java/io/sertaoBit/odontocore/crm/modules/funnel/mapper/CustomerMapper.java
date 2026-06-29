package io.sertaoBit.odontocore.crm.modules.funnel.mapper;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.provider.CustomerView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mappings({
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "address", ignore = true),
            @Mapping(target = "anonymized", ignore = true),
            @Mapping(target = "clinicId", ignore = true)
    })
    Customer toEntity(CustomerCreateRequestDTO dto);


    CustomerResponseDTO toResponseDTO(Customer customer);

    CustomerView toView(Customer customer);
}
