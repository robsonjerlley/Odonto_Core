package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.ContactLogMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.ContactLogServiceImpl;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.READ;
import static io.sertaoBit.odontocore.crm.core.enums.ContactChannel.FACEBOOK;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.CONTACT_LOG;
import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.IN_CONTACT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactLogService - Testes Unitários do Serviço")
class ContactLogServiceTest {

    private ContactLogService contactLogService;

    @Mock private ContactLogRepository contactLogRepository;
    @Mock private ContactLogMapper contactLogMapper;
    @Mock private LeadTicketRepository leadTicketRepository;
    @Mock private PermissionService permissionService;
    @Mock private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        contactLogService = new ContactLogServiceImpl(
                contactLogRepository,
                contactLogMapper,
                leadTicketRepository,
                securityUtils,
                permissionService
        );
    }

    // ========== CREATE ==========

    @Test
    @DisplayName("Deve criar um contactLog com sucesso")
    void create() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).sector(Sector.LEADS).build();
        when(securityUtils.getCurrentUser()).thenReturn(user);

        UUID ticketId = UUID.randomUUID();
        LeadTicket leadTicket = LeadTicket.builder()
                .id(ticketId)
                .createdBy(userId)
                .status(IN_CONTACT)
                .build();

        when(leadTicketRepository.findById(ticketId)).thenReturn(Optional.of(leadTicket));

        ContactLogCreateRequestDTO dto = new ContactLogCreateRequestDTO(
                ticketId,
                FACEBOOK, "Cliente atingindo pela promoção de junho",
                LocalDateTime.now()
        );

        ContactLog contactLog = ContactLog.builder()
                .id(UUID.randomUUID())
                .ticketId(dto.ticketId())
                .userId(userId)
                .channel(dto.channel())
                .note(dto.note())
                .statusBefore(leadTicket.getStatus())
                .createdAt(LocalDateTime.now())
                .occurredAt(dto.occurredAt())
                .build();

        when(contactLogRepository.save(any(ContactLog.class))).thenReturn(contactLog);

        ContactLogResponseDTO expectedDTO = new ContactLogResponseDTO(
                contactLog.getId(),
                contactLog.getTicketId(),
                contactLog.getUserId(),
                contactLog.getChannel(),
                contactLog.getNote(),
                contactLog.getStatusBefore(),
                null,
                contactLog.getOccurredAt(),
                null
        );

        when(contactLogMapper.toResponseDTO(any(ContactLog.class))).thenReturn(expectedDTO);
        ContactLogResponseDTO result = contactLogService.create(dto);

        assertNotNull(result);
        assertEquals(dto.ticketId(), result.ticketId());
        assertEquals(dto.channel(), result.channel());
        assertEquals(dto.note(), result.note());
        assertEquals(dto.occurredAt(), result.occurredAt());

        verify(contactLogRepository, times(1)).save(any(ContactLog.class));
        verify(securityUtils, times(1)).getCurrentUser();
        verify(contactLogMapper, times(1)).toResponseDTO(contactLog);
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException quando o usuário não tem permissão")
    void create_shouldThrow_WhenNotPermitted() {
        when(securityUtils.getCurrentUser()).thenReturn(new User());

        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).checkOrThrow(any(), any(), any(), any(), any());

        ContactLogCreateRequestDTO dto = new ContactLogCreateRequestDTO(
                UUID.randomUUID(),
                FACEBOOK, "Cliente atingindo pela promoção de junho",
                LocalDateTime.now()
        );

        assertThrows(AccessDeniedException.class, () -> contactLogService.create(dto));

        verify(contactLogRepository, never()).save(any());
    }

    // ========== SEARCH (ADR-003 — findByTicketId virou search com Pageable) ==========

    @Test
    @DisplayName("Deve retornar página de contactLogs pelo ticketId com sucesso")
    void search_byTicketId_success() {
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).sector(Sector.LEADS).build();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(user, CONTACT_LOG, READ)).thenReturn(Optional.of(PermissionScope.GLOBAL));

        ContactLog contactLog = ContactLog.builder()
                .id(UUID.randomUUID())
                .ticketId(ticketId)
                .userId(userId)
                .channel(FACEBOOK)
                .note("Cliente atingido pela promoção de junho")
                .statusBefore(IN_CONTACT)
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();

        ContactLogResponseDTO expectedDTO = new ContactLogResponseDTO(
                contactLog.getId(),
                contactLog.getTicketId(),
                contactLog.getUserId(),
                contactLog.getChannel(),
                contactLog.getNote(),
                contactLog.getStatusBefore(),
                null,
                contactLog.getOccurredAt(),
                null
        );

        Page<ContactLog> page = new PageImpl<>(List.of(contactLog));
        when(contactLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(expectedDTO);

        Page<ContactLogResponseDTO> result = contactLogService.search(ticketId, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(ticketId, result.getContent().get(0).ticketId());

        verify(contactLogRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(contactLogMapper, times(1)).toResponseDTO(contactLog);
    }

    // ========== FIND BY ID ==========

    @Test
    @DisplayName("Deve retornar contactLog pelo Id com sucesso")
    void findById() {
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).sector(Sector.LEADS).build();
        when(securityUtils.getCurrentUser()).thenReturn(user);

        ContactLog contactLog = ContactLog.builder()
                .id(UUID.randomUUID())
                .ticketId(ticketId)
                .userId(userId)
                .channel(FACEBOOK)
                .note("Cliente atingido pela promoção de junho")
                .statusBefore(IN_CONTACT)
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();

        ContactLogResponseDTO expectedDTO = new ContactLogResponseDTO(
                contactLog.getId(),
                contactLog.getTicketId(),
                contactLog.getUserId(),
                contactLog.getChannel(),
                contactLog.getNote(),
                contactLog.getStatusBefore(),
                null,
                contactLog.getOccurredAt(),
                null
        );

        when(contactLogRepository.findById(contactLog.getId())).thenReturn(Optional.of(contactLog));
        when(contactLogMapper.toResponseDTO(contactLog)).thenReturn(expectedDTO);

        ContactLogResponseDTO result = contactLogService.findById(contactLog.getId());

        assertNotNull(result);
        assertEquals(contactLog.getId(), result.id());

        verify(contactLogRepository, times(1)).findById(contactLog.getId());
        verify(contactLogMapper, times(1)).toResponseDTO(contactLog);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando contactLog não encontrado por Id")
    void findById_shouldThrow_WhenNotFound() {
        UUID id = UUID.randomUUID();

        when(contactLogRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contactLogService.findById(id));

        verify(contactLogRepository, times(1)).findById(id);
        verify(contactLogMapper, never()).toResponseDTO(any());
    }
}