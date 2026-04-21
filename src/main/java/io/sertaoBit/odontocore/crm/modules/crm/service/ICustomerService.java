package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.CustomerResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
public interface ICustomerService {

    CustomerResponseDTO create(CustomerCreateRequestDTO dto);

    CustomerResponseDTO update(String cpf, CustomerUpdateRequestDTO dto);

    List<CustomerResponseDTO> findAll();

    CustomerResponseDTO findById(UUID id);

    CustomerResponseDTO findByName(String name);

    CustomerResponseDTO findByCPF(String cpf);

    void deleteById(UUID id);


}
