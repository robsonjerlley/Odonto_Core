package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;

import java.util.List;
import java.util.UUID;


public interface CustomerService {

    CustomerResponseDTO create(CustomerCreateRequestDTO dto);

    CustomerResponseDTO update(UUID id, CustomerUpdateRequestDTO dto);

    List<CustomerResponseDTO> findAll();

    CustomerResponseDTO findById(UUID id);

    List<CustomerResponseDTO> findByName(String name);

    CustomerResponseDTO findByCpf(String cpf);

      List<CustomerResponseDTO> findByAdChannel(AdsChannel  channel);

    void deleteById(UUID id);


}
