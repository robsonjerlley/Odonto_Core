package io.sertaoBit.odontocore.crm.modules.identity.mapper;

import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface IUserMapper {

    @Mapping(target = "id" , ignore = true)
    @Mapping(target = "clinic",  ignore = true)
    User toEntity(UserCreateRequestDTO dto);


    UserResponseDTO toResponseDTO(User user);

}
