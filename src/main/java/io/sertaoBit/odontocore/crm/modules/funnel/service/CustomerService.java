package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;


public interface CustomerService {

    CustomerResponseDTO create(CustomerCreateRequestDTO dto);

    CustomerResponseDTO update(UUID id, CustomerUpdateRequestDTO dto);

    Page<CustomerResponseDTO> search(String phone, String name, AdsChannel adChannel, Pageable pageable);

    CustomerResponseDTO findById(UUID id);

    CustomerResponseDTO findByCpf(String cpf);

    void deleteById(UUID id);


}
