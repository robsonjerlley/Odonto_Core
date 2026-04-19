package io.sertaoBit.odontocore.crm.modules.identity.mapper;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {


    User toEntity(UserCreateRequestDTO dto);

    UserResponseDTO toResponseDTO(User user);

}
