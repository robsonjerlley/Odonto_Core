package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.ApplyDiscountRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.mapper.DealMapper;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.service.impl.DealServiceImpl;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import io.sertaoBit.odontocore.crm.shared.DealProcedureDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DealService - Testes Unitários")
public class DealServiceTest {

    private DealService dealService;
    @Mock private DealRepository dealRepository;
    @Mock private LeadTicketRepository ticketRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private PermissionService permissionService;
    @Mock private DealMapper dealMapper;
    @Mock private DealHistoryService dealHistoryService;

    @BeforeEach
    void setUp() {
        dealService = new DealServiceImpl(
                dealRepository, ticketRepository,
                securityUtils, permissionService,
                dealMapper, dealHistoryService);
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .username("test@test.com")
                .passwordHash("hash")
                .sector(Sector.EVALUATOR)
                .role(Role.USER_EVALUATOR)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve criar deal com totalValue calculado corretamente e ticket → NEGOTIATION")
    void create_success() {
        UUID ticketId = UUID.randomUUID();
        User user = buildUser();

        LeadTicket ticket = LeadTicket.builder().id(ticketId).status(IN_EVALUATION).build();

        DealCreateRequestDTO dto = new DealCreateRequestDTO(List.of(
                new DealProcedureDTO("Implante", null, new BigDecimal("1000.00"), 2, null),
                new DealProcedureDTO("Consulta", null, new BigDecimal("200.00"), 1, null)
        ));

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(true);
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(dealRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Deal result = dealService.create(ticketId, dto);

        assertEquals(new BigDecimal("2200.00"), result.getTotalValue());
        assertEquals(NEGOTIATION, ticket.getStatus());
        assertEquals(Sector.COMMERCIAL, ticket.getCurrentSector());
        verify(dealRepository).save(any(Deal.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao criar deal com ticket inexistente")
    void create_ticketNotFound() {
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dealService.create(UUID.randomUUID(), new DealCreateRequestDTO(List.of())));

        verify(dealRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar AccessDeniedException ao criar deal sem permissão")
    void create_noPermission() {
        UUID ticketId = UUID.randomUUID();
        LeadTicket ticket = LeadTicket.builder().id(ticketId).status(IN_EVALUATION).build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> dealService.create(ticketId, new DealCreateRequestDTO(List.of())));

        verify(dealRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao criar deal com ticket em status inválido")
    void create_invalidStatus() {
        UUID ticketId = UUID.randomUUID();
        LeadTicket ticket = LeadTicket.builder().id(ticketId).status(NEW).build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> dealService.create(ticketId, new DealCreateRequestDTO(List.of())));
    }

    @Test
    @DisplayName("Deve calcular finalValue corretamente ao aplicar desconto (HALF_UP)")
    void applyDiscount_success() {
        UUID dealId = UUID.randomUUID();
        Deal deal = Deal.builder()
                .id(dealId)
                .totalValue(new BigDecimal("1000.00"))
                .archived(false)
                .build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(true);
        when(dealRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Deal result = dealService.applyDiscount(dealId, new ApplyDiscountRequestDTO(new BigDecimal("10")));

        assertEquals(new BigDecimal("900.00"), result.getFinalValue());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao aplicar desconto em deal arquivado")
    void applyDiscount_dealArchived() {
        UUID dealId = UUID.randomUUID();
        Deal deal = Deal.builder().id(dealId).archived(true).build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> dealService.applyDiscount(dealId, new ApplyDiscountRequestDTO(new BigDecimal("10"))));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao aplicar desconto negativo")
    void applyDiscount_negativePct() {
        UUID dealId = UUID.randomUUID();
        Deal deal = Deal.builder().id(dealId).archived(false).totalValue(new BigDecimal("1000.00")).build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> dealService.applyDiscount(dealId, new ApplyDiscountRequestDTO(new BigDecimal("-1"))));
    }

    @Test
    @DisplayName("Deve fechar deal, setar closedAt e mover ticket para WIN")
    void closeDeal_success() {
        UUID dealId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        Deal deal = Deal.builder()
                .id(dealId)
                .ticketId(ticketId)
                .archived(false)
                .build();

        LeadTicket ticket = LeadTicket.builder().id(ticketId).status(NEGOTIATION).build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(true);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(dealRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Deal result = dealService.closeDeal(dealId, "PIX");

        assertNotNull(result.getClosedAt());
        assertEquals(WIN, ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao fechar deal já fechado")
    void closeDeal_alreadyClosed() {
        UUID dealId = UUID.randomUUID();
        Deal deal = Deal.builder()
                .id(dealId)
                .archived(false)
                .closedAt(LocalDateTime.now())
                .build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> dealService.closeDeal(dealId, "PIX"));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao fechar deal arquivado")
    void closeDeal_archived() {
        UUID dealId = UUID.randomUUID();
        Deal deal = Deal.builder().id(dealId).archived(true).build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(permissionService.canAccess(any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> dealService.closeDeal(dealId, "PIX"));
    }
}
