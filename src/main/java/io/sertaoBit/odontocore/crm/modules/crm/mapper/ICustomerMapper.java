package io.sertaoBit.odontocore.crm.modules.crm.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ICustomerMapper {


    @Mapping(target = "id", ignore = true )
    Customer toEntity(CustomerCreateRequestDTO dto);


    CustomerResponseDTO toResponseDTO(Customer customer);
}
