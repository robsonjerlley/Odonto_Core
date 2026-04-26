package io.sertaoBit.odontocore.crm.modules.crm.service;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.customer.CustomerUpdateRequestDTO;
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

    List<CustomerResponseDTO> findByName(String name);

    CustomerResponseDTO findByCpf(String cpf);

    void deleteById(UUID id);


}
