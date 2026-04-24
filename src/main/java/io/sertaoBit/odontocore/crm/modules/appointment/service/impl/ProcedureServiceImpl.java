package io.sertaoBit.odontocore.crm.modules.appointment.service.impl;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.ProcedureCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.ProcedureUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Procedure;
import io.sertaoBit.odontocore.crm.modules.appointment.mapper.IProcedureMapper;
import io.sertaoBit.odontocore.crm.modules.appointment.repositoy.IProcedureRepository;
import io.sertaoBit.odontocore.crm.modules.appointment.service.IProcedureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProcedureServiceImpl implements IProcedureService {

    private final IProcedureRepository procedureRepository;
    private final IProcedureMapper procedureMapper;

    public ProcedureServiceImpl(IProcedureRepository procedureRepository, IProcedureMapper procedureMapper) {
        this.procedureRepository = procedureRepository;
        this.procedureMapper = procedureMapper;
    }


    @Override
    @Transactional
    public ProcedureResponseDTO create(ProcedureCreateRequestDTO dto) {
        Procedure procedure = procedureMapper.toEntity(dto);
        return procedureMapper.toResponseDTO(procedureRepository.save(procedure));
    }

    @Override
    @Transactional
    public ProcedureResponseDTO update(UUID id, ProcedureUpdateRequestDTO dto) {
        Procedure procedure = procedureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Procedure not found" + id));

        procedure.setName(dto.name());
        procedure.setDescription(dto.description());
        procedure.setBasePrice(dto.basePrice());
        procedure.setCategory(dto.category());

        return procedureMapper.toResponseDTO(procedureRepository.save(procedure));
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedureResponseDTO findById(UUID id) {
        return procedureRepository.findById(id)
                .map(procedureMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Procedure not found" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureResponseDTO> findAll() {
        return procedureRepository.findAll().stream()
                .map(procedureMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureResponseDTO> findByName(String name) {
        return procedureRepository.findByName(name)
                .stream().map(procedureMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public void delete(UUID id) {
        if (!procedureRepository.existsById(id)) {
            throw new RuntimeException("Procedure not found" + id);
        }
        procedureRepository.deleteById(id);
    }
}
