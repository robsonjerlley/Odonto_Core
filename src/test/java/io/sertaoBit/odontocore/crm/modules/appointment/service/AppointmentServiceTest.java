package io.sertaoBit.odontocore.crm.modules.appointment.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.AssigneeRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.BatchItem;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.CancelRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.RescheduleRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.request.ScheduleRequestDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.BatchScheduleResultDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import io.sertaoBit.odontocore.crm.modules.appointment.mapper.AppointmentMapper;
import io.sertaoBit.odontocore.crm.modules.appointment.repository.AppointmentRepository;
import io.sertaoBit.odontocore.crm.modules.appointment.service.impl.AppointmentServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService - Testes Unitários")
class AppointmentServiceTest {

    private AppointmentService appointmentService;

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository, appointmentMapper, securityUtils, permissionService);
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Avaliador")
                .username("avaliador@test.com")
                .password("hash")
                .sector(Sector.EVALUATOR)
                .role(Role.USER_EVALUATOR)
                .active(true)
                .build();
    }

    private Appointment buildAppointment(UUID id, io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus status) {
        return Appointment.builder()
                .id(id)
                .status(status)
                .assignedTo(UUID.randomUUID())
                .build();
    }

    // ========== SCHEDULE ==========

    @Test
    @DisplayName("schedule - deve agendar appointment AWAITING_SCHEDULE → SCHEDULED")
    void schedule_success() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, AWAITING_SCHEDULE);
        LocalDateTime when = LocalDateTime.now().plusDays(1);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appointmentService.schedule(id, new ScheduleRequestDTO(when, null));

        assertEquals(SCHEDULED, appointment.getStatus());
        assertEquals(when, appointment.getScheduledAt());
    }

    @Test
    @DisplayName("schedule - deve sobrescrever assignedTo quando informado no DTO")
    void schedule_overridesAssignee() {
        UUID id = UUID.randomUUID();
        UUID newAssignee = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, AWAITING_SCHEDULE);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appointmentService.schedule(id, new ScheduleRequestDTO(LocalDateTime.now().plusDays(1), newAssignee));

        assertEquals(newAssignee, appointment.getAssignedTo());
    }

    @Test
    @DisplayName("schedule - deve lançar ResourceNotFoundException quando appointment inexistente")
    void schedule_notFound() {
        UUID id = UUID.randomUUID();
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.schedule(id, new ScheduleRequestDTO(LocalDateTime.now().plusDays(1), null)));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("schedule - deve lançar AccessDeniedException sem permissão")
    void schedule_noPermission() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, AWAITING_SCHEDULE);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).checkOrThrow(any(), any(), any(), any(), any());

        assertThrows(AccessDeniedException.class,
                () -> appointmentService.schedule(id, new ScheduleRequestDTO(LocalDateTime.now().plusDays(1), null)));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("schedule - deve lançar IllegalArgumentException com data no passado")
    void schedule_pastDate() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, AWAITING_SCHEDULE);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalArgumentException.class,
                () -> appointmentService.schedule(id, new ScheduleRequestDTO(LocalDateTime.now().minusDays(1), null)));
    }

    @Test
    @DisplayName("schedule - deve lançar IllegalStateException se status não for AWAITING_SCHEDULE")
    void schedule_invalidStatus() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, SCHEDULED);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> appointmentService.schedule(id, new ScheduleRequestDTO(LocalDateTime.now().plusDays(1), null)));
    }

    // ========== RESCHEDULE ==========

    @Test
    @DisplayName("reschedule - deve reagendar appointment SCHEDULED")
    void reschedule_success() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, SCHEDULED);
        LocalDateTime when = LocalDateTime.now().plusDays(2);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appointmentService.reschedule(id, new RescheduleRequestDTO(when));

        assertEquals(SCHEDULED, appointment.getStatus());
        assertEquals(when, appointment.getScheduledAt());
    }

    @Test
    @DisplayName("reschedule - deve lançar IllegalStateException se status não for SCHEDULED")
    void reschedule_invalidStatus() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, AWAITING_SCHEDULE);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> appointmentService.reschedule(id, new RescheduleRequestDTO(LocalDateTime.now().plusDays(1))));
    }

    @Test
    @DisplayName("reschedule - deve lançar IllegalArgumentException com data no passado")
    void reschedule_pastDate() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, SCHEDULED);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalArgumentException.class,
                () -> appointmentService.reschedule(id, new RescheduleRequestDTO(LocalDateTime.now().minusDays(1))));
    }

    // ========== ASSIGNEE ==========

    @Test
    @DisplayName("assignee - deve trocar o responsável do appointment")
    void assignee_success() {
        UUID id = UUID.randomUUID();
        UUID newAssignee = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, AWAITING_SCHEDULE);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appointmentService.assignee(id, new AssigneeRequestDTO(newAssignee));

        assertEquals(newAssignee, appointment.getAssignedTo());
    }

    @Test
    @DisplayName("assignee - deve lançar IllegalArgumentException com assignedTo nulo")
    void assignee_nullAssignee() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, AWAITING_SCHEDULE);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalArgumentException.class,
                () -> appointmentService.assignee(id, new AssigneeRequestDTO(null)));
        verify(appointmentRepository, never()).save(any());
    }

    // ========== CANCEL ==========

    @Test
    @DisplayName("cancel - deve cancelar e registrar o motivo")
    void cancel_success() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, SCHEDULED);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appointmentService.cancel(id, new CancelRequestDTO("Paciente desistiu"));

        assertEquals(CANCELLED, appointment.getStatus());
        assertEquals("Paciente desistiu", appointment.getCancelReason());
    }

    @Test
    @DisplayName("cancel - deve lançar IllegalArgumentException com motivo em branco")
    void cancel_blankReason() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, SCHEDULED);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalArgumentException.class,
                () -> appointmentService.cancel(id, new CancelRequestDTO("   ")));
        verify(appointmentRepository, never()).save(any());
    }

    // ========== COMPLETE ==========

    @Test
    @DisplayName("complete - deve concluir appointment SCHEDULED → DONE")
    void complete_success() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, SCHEDULED);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        appointmentService.complete(id);

        assertEquals(DONE, appointment.getStatus());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    @DisplayName("complete - deve lançar IllegalStateException se status não for SCHEDULED")
    void complete_invalidStatus() {
        UUID id = UUID.randomUUID();
        Appointment appointment = buildAppointment(id, AWAITING_SCHEDULE);

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class, () -> appointmentService.complete(id));
        verify(appointmentRepository, never()).save(any());
    }

    // ========== SCHEDULE BATCH ==========

    @Test
    @DisplayName("scheduleBatch - deve agendar todos sem conflito")
    void scheduleBatch_success() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Appointment a1 = buildAppointment(id1, AWAITING_SCHEDULE);
        Appointment a2 = buildAppointment(id2, AWAITING_SCHEDULE);

        LocalDateTime slot1 = LocalDateTime.now().plusDays(1);
        LocalDateTime slot2 = LocalDateTime.now().plusDays(2);
        List<BatchItem> items = List.of(
                new BatchItem(id1, slot1, null),
                new BatchItem(id2, slot2, null));

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findAllById(any())).thenReturn(List.of(a1, a2));
        when(appointmentRepository.findByStatusAndAssignedToInAndScheduledAtIn(eq(SCHEDULED), anyCollection(), anyCollection()))
                .thenReturn(List.of());
        when(appointmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        BatchScheduleResultDTO result = appointmentService.scheduleBatch(items);

        assertTrue(result.warnings().isEmpty());
        assertEquals(2, result.scheduled().size());
        assertEquals(SCHEDULED, a1.getStatus());
        assertEquals(SCHEDULED, a2.getStatus());
        assertEquals(slot1, a1.getScheduledAt());
        assertEquals(slot2, a2.getScheduledAt());
    }

    @Test
    @DisplayName("scheduleBatch - deve gerar warning quando 2 no mesmo (executor, horário)")
    void scheduleBatch_conflictWarning() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        Appointment a1 = buildAppointment(id1, AWAITING_SCHEDULE);
        Appointment a2 = buildAppointment(id2, AWAITING_SCHEDULE);

        LocalDateTime slot = LocalDateTime.now().plusDays(1);
        List<BatchItem> items = List.of(
                new BatchItem(id1, slot, assignee),
                new BatchItem(id2, slot, assignee));

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findAllById(any())).thenReturn(List.of(a1, a2));
        when(appointmentRepository.findByStatusAndAssignedToInAndScheduledAtIn(eq(SCHEDULED), anyCollection(), anyCollection()))
                .thenReturn(List.of());
        when(appointmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        BatchScheduleResultDTO result = appointmentService.scheduleBatch(items);

        assertEquals(1, result.warnings().size());
        assertEquals(assignee, result.warnings().get(0).assignedTo());
        assertEquals(2, result.warnings().get(0).appointmentIds().size());
    }

    @Test
    @DisplayName("scheduleBatch - deve lançar IllegalArgumentException com appointmentId duplicado")
    void scheduleBatch_duplicateId() {
        UUID id = UUID.randomUUID();
        LocalDateTime slot = LocalDateTime.now().plusDays(1);
        List<BatchItem> items = List.of(
                new BatchItem(id, slot, null),
                new BatchItem(id, slot, null));

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());

        assertThrows(IllegalArgumentException.class, () -> appointmentService.scheduleBatch(items));
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("scheduleBatch - deve lançar IllegalArgumentException quando algum id não existe")
    void scheduleBatch_notFound() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<BatchItem> items = List.of(
                new BatchItem(id1, LocalDateTime.now().plusDays(1), null),
                new BatchItem(id2, LocalDateTime.now().plusDays(1), null));

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findAllById(any()))
                .thenReturn(List.of(buildAppointment(id1, AWAITING_SCHEDULE)));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.scheduleBatch(items));
        verify(appointmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("scheduleBatch - deve lançar IllegalStateException se algum item não está AWAITING_SCHEDULE")
    void scheduleBatch_invalidStatus() {
        UUID id = UUID.randomUUID();
        List<BatchItem> items = List.of(new BatchItem(id, LocalDateTime.now().plusDays(1), null));

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(appointmentRepository.findAllById(any()))
                .thenReturn(List.of(buildAppointment(id, SCHEDULED)));

        assertThrows(IllegalStateException.class, () -> appointmentService.scheduleBatch(items));
        verify(appointmentRepository, never()).saveAll(any());
    }

    // ========== QUERIES PAGINADAS ==========

    @Test
    @DisplayName("getStatus - deve consultar por escopo e status")
    void getStatus_success() {
        User user = buildUser();
        Pageable pageable = PageRequest.of(0, 10);

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any()))
                .thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(appointmentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<?> result = appointmentService.getStatus(SCHEDULED, pageable);

        assertNotNull(result);
        verify(appointmentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getStatus - deve lançar AccessDeniedException quando sem escopo de leitura")
    void getStatus_noScope() {
        User user = buildUser();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> appointmentService.getStatus(SCHEDULED, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("getAssignedBetween - deve consultar por escopo, responsável e intervalo")
    void getAssignedBetween_success() {
        User user = buildUser();
        Pageable pageable = PageRequest.of(0, 10);

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any()))
                .thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(appointmentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<?> result = appointmentService.getAssignedBetween(
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now().plusDays(7), pageable);

        assertNotNull(result);
        verify(appointmentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getAssignedBetween - deve lançar AccessDeniedException quando sem escopo de leitura")
    void getAssignedBetween_noScope() {
        User user = buildUser();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> appointmentService.getAssignedBetween(
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now().plusDays(7), PageRequest.of(0, 10)));
    }
}
