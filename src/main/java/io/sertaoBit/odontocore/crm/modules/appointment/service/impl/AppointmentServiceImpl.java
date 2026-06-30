package io.sertaoBit.odontocore.crm.modules.appointment.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.*;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.AppointmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.BatchScheduleResultDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.ConflictWarning;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import io.sertaoBit.odontocore.crm.modules.appointment.mapper.AppointmentMapper;
import io.sertaoBit.odontocore.crm.modules.appointment.repository.AppointmentRepository;
import io.sertaoBit.odontocore.crm.modules.appointment.repository.AppointmentSpecifications;
import io.sertaoBit.odontocore.crm.modules.appointment.service.AppointmentService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.sertaoBit.odontocore.crm.core.enums.Action.READ;
import static io.sertaoBit.odontocore.crm.core.enums.Action.UPDATE;
import static io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus.*;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.APPOINTMENT;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.EVALUATOR;
import static io.sertaoBit.odontocore.crm.modules.appointment.repository.AppointmentSpecifications.byScope;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            AppointmentMapper appointmentMapper,
            SecurityUtils securityUtils,
            PermissionService permissionService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
        this.securityUtils = securityUtils;
        this.permissionService = permissionService;
    }


    @Override
    @Transactional
    public AppointmentResponseDTO schedule(UUID id, ScheduleRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        permissionService.checkOrThrow(
                user,
                APPOINTMENT,
                UPDATE,
                EVALUATOR,
                appointment.getAssignedTo()
        );

        if (dto.scheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid scheduled at");
        }

        if (appointment.getStatus() != AWAITING_SCHEDULE) {
            throw new IllegalStateException("Invalid scheduling status");
        }

        appointment.setStatus(SCHEDULED);
        if(dto.assignedTo() != null) {
            appointment.setAssignedTo(dto.assignedTo());
        }
        appointment.setScheduledAt(dto.scheduledAt());

        return appointmentMapper.toResponseDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponseDTO reschedule(UUID id, RescheduleRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        permissionService.checkOrThrow(
                user,
                APPOINTMENT,
                UPDATE,
                EVALUATOR,
                appointment.getAssignedTo()
        );

        if(dto.scheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid scheduled at");
        }

        if (appointment.getStatus() != SCHEDULED) {
            throw new IllegalStateException("Invalid scheduling status");
        }

        appointment.setStatus(SCHEDULED);
        appointment.setScheduledAt(dto.scheduledAt());

        return appointmentMapper.toResponseDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponseDTO assignee(UUID id, AssigneeRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        permissionService.checkOrThrow(
                user,
                APPOINTMENT,
                UPDATE,
                EVALUATOR,
                appointment.getAssignedTo()
        );

        // CASO CONTROLLER FALHE EM BARRAR POR MEIO DO @VALID
        if(dto.assignedTo() == null) {
            throw new IllegalArgumentException("Invalid assigned user");
        }

        appointment.setAssignedTo(dto.assignedTo());

        return appointmentMapper.toResponseDTO(appointmentRepository.save(appointment));
    }


    @Override
    @Transactional
    public AppointmentResponseDTO cancel(UUID id, CancelRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        permissionService.checkOrThrow(
                user,
                APPOINTMENT,
                UPDATE,
                EVALUATOR,
                appointment.getAssignedTo()
        );

        if(dto.cancelReason().isBlank()) {
            throw new IllegalArgumentException("Cancel reason is necessary");
        }

        appointment.setStatus(CANCELLED);
        appointment.setCancelReason(dto.cancelReason());

        return appointmentMapper.toResponseDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public BatchScheduleResultDTO scheduleBatch(List<BatchItem> items) {
        User user = securityUtils.getCurrentUser();

        List<UUID> ids = items.stream().map(BatchItem::appointmentId).toList();

        Set<UUID> distinctIds = new HashSet<>(ids);
        if (distinctIds.size() != ids.size()) {
            throw new IllegalArgumentException("Duplicate appointmentId in batch");
        }

        Map<UUID, Appointment> byId = appointmentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Appointment::getId, a -> a));

        if (byId.size() != ids.size()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        validateItems(items, byId, user);

        List<ConflictWarning> warnings = getConflictWarnings(items, byId);

        return new BatchScheduleResultDTO(applyAndSave(items, byId), warnings);
    }



    private @NonNull List<AppointmentResponseDTO> applyAndSave(List<BatchItem> items, Map<UUID, Appointment> byId) {
        for (BatchItem item : items) {
            Appointment ap = byId.get(item.appointmentId());
            ap.setStatus(SCHEDULED);
            ap.setScheduledAt(item.scheduledAt());
            if (item.assignedTo() != null) {
                ap.setAssignedTo(item.assignedTo());
            }
        }

        List<Appointment> saved = appointmentRepository.saveAll(
                items.stream().map(i -> byId.get(i.appointmentId())).toList());

        return saved.stream()
                .map(appointmentMapper::toResponseDTO).toList();
    }

    private @NonNull List<ConflictWarning> getConflictWarnings(List<BatchItem> items, Map<UUID, Appointment> byId) {
        record Slot(UUID assignedTo, LocalDateTime at) {}

        Map<UUID, UUID> effective = new HashMap<>();
        for (BatchItem item : items) {
            UUID eff = item.assignedTo() != null
                    ? item.assignedTo()
                    : byId.get(item.appointmentId()).getAssignedTo();
            effective.put(item.appointmentId(), eff);
        }

        Set<UUID> assignees = new HashSet<>(effective.values());
        Set<LocalDateTime> slots = items.stream().map(BatchItem::scheduledAt).collect(Collectors.toSet());

        List<Appointment> existing = appointmentRepository
                .findByStatusAndAssignedToInAndScheduledAtIn(SCHEDULED, assignees, slots);

        Map<Slot, List<UUID>> bySlot = new HashMap<>();
        for (BatchItem item : items) {
            Slot key = new Slot(effective.get(item.appointmentId()), item.scheduledAt());
            bySlot.computeIfAbsent(key, k -> new ArrayList<>()).add(item.appointmentId());
        }
        for (Appointment ap : existing) {
            Slot key = new Slot(ap.getAssignedTo(), ap.getScheduledAt());
            if (bySlot.containsKey(key)) {
                bySlot.get(key).add(ap.getId());
            }
        }

        return bySlot.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)   // 2+ no mesmo (executor, horário) = aviso
                .map(e -> new ConflictWarning(e.getKey().assignedTo(), e.getKey().at(), e.getValue()))
                .toList();
    }

    private void validateItems(List<BatchItem> items, Map<UUID, Appointment> byId, User user) {
        for (BatchItem item : items) {
            Appointment ap = byId.get(item.appointmentId());
            permissionService.checkOrThrow(user, APPOINTMENT, UPDATE, EVALUATOR, ap.getAssignedTo());
            if (ap.getStatus() != AWAITING_SCHEDULE) {
                throw new IllegalStateException("Appointment is not awaiting schedule");
            }
            if (item.scheduledAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Invalid scheduled at");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> getStatus(AppointmentStatus status, Pageable page) {
        User user = securityUtils.getCurrentUser();
      PermissionScope scope = permissionService.getScope(
                user,
                APPOINTMENT,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access denied"));

        Specification<Appointment> spec = Specification
                .where(byScope(scope, user))
                .and(AppointmentSpecifications.hasStatus(status));

        return appointmentRepository.findAll(spec,page)
                .map(appointmentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> getAssignedBetween(UUID assignedI, LocalDateTime from, LocalDateTime to, Pageable page) {
        User user = securityUtils.getCurrentUser();
        PermissionScope scope = permissionService.getScope(
                user,
                APPOINTMENT,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access denied"));

        Specification<Appointment> spec = Specification
                .where(byScope(scope,user))
                .and(AppointmentSpecifications.assignedTo(assignedI))
                .and(AppointmentSpecifications.scheduledBetween(from, to));

        return appointmentRepository.findAll(spec,page)
                .map(appointmentMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public void complete(UUID id) {
        User user = securityUtils.getCurrentUser();
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        permissionService.checkOrThrow(
                user,
                APPOINTMENT,
                UPDATE,
                EVALUATOR,
                appointment.getAssignedTo()
        );

        if(appointment.getStatus() != SCHEDULED) {
            throw new IllegalStateException("Invalid scheduling status");
        }

        appointment.setStatus(DONE);
        appointmentRepository.save(appointment);
    }
}
