package io.sertaoBit.odontocore.crm.modules.appointment.service.impl;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.AppointmentCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.AppointmentUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.AppointmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import io.sertaoBit.odontocore.crm.modules.appointment.mapper.IAppointmentMapper;
import io.sertaoBit.odontocore.crm.modules.appointment.repositoy.IAppointmentRepository;
import io.sertaoBit.odontocore.crm.modules.appointment.service.IAppointmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements IAppointmentService {

    private final IAppointmentRepository appointmentRepository;
    private final IAppointmentMapper appointmentMapper;

    public AppointmentServiceImpl(IAppointmentRepository appointmentRepository,
                                  IAppointmentMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @Override
    @Transactional
    public AppointmentResponseDTO create(AppointmentCreateRequestDTO dto) {
        Appointment appointment = appointmentMapper.toEntity(dto);
        return appointmentMapper.toResponseDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponseDTO update(UUID id, AppointmentUpdateRequestDTO dto) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ID not found." + id));

        appointment.setStatus(dto.status());
        appointment.setClinic(dto.clinic());
        appointment.setUser(dto.user());
        appointment.setCustomer(dto.customer());
        appointment.setProcedures(dto.procedures());
        appointment.setTotalValue(dto.totalValue());

        return appointmentMapper.toResponseDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> findAll() {
        return appointmentRepository.findAll()
                .stream().map(appointmentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDTO findById(UUID id) {
        return appointmentRepository.findById(id)
                .map(appointmentMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("ID not found." + id));
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteById(UUID id) {
        if (!appointmentRepository.existsById(id)) {
            throw new RuntimeException("ID not found." + id);
        }
        appointmentRepository.deleteById(id);
    }
}
