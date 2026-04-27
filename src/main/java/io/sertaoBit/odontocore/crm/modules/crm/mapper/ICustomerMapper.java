package io.sertaoBit.odontocore.crm.modules.crm.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ICustomerMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "createdByUser", ignore = true),
            @Mapping(target = "department", ignore = true),
            @Mapping(target = "descriptions", source = "description", qualifiedByName = "stringToList")
    })
    Customer toEntity(CustomerCreateRequestDTO dto);


    @Mappings({
            @Mapping(target = "userId", source = "createdByUser.id"),
            @Mapping(target = "description", source = "descriptions")
    })
    CustomerResponseDTO toResponseDTO(Customer customer);

    @Named("stringToList")
    default List<String> stringToList(String description) {
        return description != null && !description.isBlank()
                ? List.of(description)
                : List.of();
    }
}
