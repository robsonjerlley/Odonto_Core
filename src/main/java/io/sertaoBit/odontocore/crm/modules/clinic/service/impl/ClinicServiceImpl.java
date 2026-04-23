package io.sertaoBit.odontocore.crm.modules.clinic.service.impl;

import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.request.ClinicUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.api.dto.response.ClinicResponseDTO;
import io.sertaoBit.odontocore.crm.modules.clinic.domain.model.Clinic;
import io.sertaoBit.odontocore.crm.modules.clinic.mapper.IClinicMapper;
import io.sertaoBit.odontocore.crm.modules.clinic.repository.IClinicRepository;
import io.sertaoBit.odontocore.crm.modules.clinic.service.IClinicService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClinicServiceImpl implements IClinicService {

    private final IClinicRepository clinicRepository;
    private final IClinicMapper clinicMapper;

    public ClinicServiceImpl(IClinicRepository clinicRepository, IClinicMapper clinicMapper) {
        this.clinicRepository = clinicRepository;
        this.clinicMapper = clinicMapper;
    }


    @Override
    @Transactional
    public ClinicResponseDTO create(ClinicCreateRequestDTO dto) {
       Clinic newClinic = clinicMapper.toEntity(dto);
       Clinic savedClinic = clinicRepository.save(newClinic);
       return clinicMapper.toResponseDTO(savedClinic);
    }

    @Override
    @Transactional
    public ClinicResponseDTO update(String cnpj, ClinicUpdateRequestDTO dto) {
      Clinic clinic =  clinicRepository.findByCnpj(cnpj)
              .orElseThrow(()-> new RuntimeException("Clinic not found. " + cnpj));

      if(!clinic.getCnpj().equals(dto.cnpj()) && clinicRepository.findByCnpj(dto.cnpj()).isPresent()){
          throw new RuntimeException("CNPJ já existente na base de dados.");
      }
      clinic.setName(dto.name());
      clinic.setCnpj(dto.cnpj());
      clinic.setTelephone(dto.telephone());
      clinic.setAddress(dto.address());
      clinic.setCity(dto.city());

        Clinic clinicToUpdate = clinicRepository.save(clinic);
        return clinicMapper.toResponseDTO(clinicToUpdate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicResponseDTO> findAll() {
        return clinicRepository.findAll().stream()
                .map(clinicMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicResponseDTO findById(UUID id) {
        return clinicRepository.findById(id)
                .map(clinicMapper::toResponseDTO)
                .orElseThrow(()-> new RuntimeException("Clinic not found. " + id));
    }

    public ClinicResponseDTO findByCnpj(String cnpj) {
        return clinicRepository.findByCnpj(cnpj)
                .map(clinicMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("CNPJ não encontrado na base de dados. " + cnpj));
    }

    @Override
    @Transactional(readOnly = true)
    public void delete(UUID id) {
        if(!clinicRepository.existsById(id)){
            throw new RuntimeException("Clinic not found. " + id);
        }
        clinicRepository.deleteById(id);
    }
}
