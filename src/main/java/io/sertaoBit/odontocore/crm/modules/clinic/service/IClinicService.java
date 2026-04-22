package io.sertaoBit.odontocore.crm.modules.clinic.service;

import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.response.ClinicResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IClinicService {

    ClinicResponseDTO create(ClinicCreateRequestDTO dto);

    ClinicResponseDTO update(String cnpj, ClinicUpdateRequestDTO dto);

    ClinicResponseDTO findById(UUID id);

    List<ClinicResponseDTO> findAll();

    List<ClinicResponseDTO> findByAllEmployees();

    void delete(UUID id);

}
