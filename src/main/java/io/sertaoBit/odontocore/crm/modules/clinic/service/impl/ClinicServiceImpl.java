package io.sertaoBit.odontocore.crm.modules.clinic.service.impl;

import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.response.ClinicResponseDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.service.IClinicService;

import java.util.List;
import java.util.UUID;

public class ClinicServiceImpl implements IClinicService {
    @Override
    public ClinicResponseDTO create(ClinicCreateRequestDTO dto) {
        return null;
    }

    @Override
    public ClinicResponseDTO update(String cnpj, ClinicUpdateRequestDTO dto) {
        return null;
    }

    @Override
    public ClinicResponseDTO findById(UUID id) {
        return null;
    }

    @Override
    public List<ClinicResponseDTO> findAll() {
        return List.of();
    }

    @Override
    public List<ClinicResponseDTO> findByAllEmployees() {
        return List.of();
    }

    @Override
    public void delete(UUID id) {

    }
}
