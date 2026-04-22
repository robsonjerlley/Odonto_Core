package io.sertaoBit.odontocore.crm.modules.crm.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface ICustomerMapper {


    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "user.id", source = "userId"),
            @Mapping(target = "description", ignore = true)
    })
    Customer toEntity(CustomerCreateRequestDTO dto);


    @Mapping(target = "userId", source = "user.id")
    CustomerResponseDTO toResponseDTO(Customer customer);
}
