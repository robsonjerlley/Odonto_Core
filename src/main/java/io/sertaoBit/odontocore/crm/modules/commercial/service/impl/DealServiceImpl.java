package io.sertaoBit.odontocore.crm.modules.commercial.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Action;
import io.sertaoBit.odontocore.crm.core.enums.Resource;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.ApplyDiscountRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.DealDetailResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.mapper.DealMapper;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.model.DealProcedure;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.service.DealHistoryService;
import io.sertaoBit.odontocore.crm.modules.commercial.service.DealService;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepository;
    private final LeadTicketRepository ticketRepository;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;
    private final DealMapper dealMapper;
    private final DealHistoryService dealHistoryService;

    public DealServiceImpl(
            DealRepository dealRepository,
            LeadTicketRepository ticketRepository,
            SecurityUtils securityUtils,
            PermissionService permissionService,
            DealMapper dealMapper,
            DealHistoryService dealHistoryService
    ) {
        this.dealRepository = dealRepository;
        this.ticketRepository = ticketRepository;
        this.securityUtils = securityUtils;
        this.permissionService = permissionService;
        this.dealMapper = dealMapper;
        this.dealHistoryService = dealHistoryService;
    }

    @Override
    @Transactional
    public Deal create(UUID ticketId, DealCreateRequestDTO dto) {
        LeadTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Not Found"));

        var currentUser = securityUtils.getCurrentUser();

        if (!permissionService.canAccess(
                currentUser, Resource.DEAL,
                Action.CREATE, Sector.EVALUATOR,
                null)
        ) {
            throw new AccessDeniedException("User not have access");
        }

        if (ticket.getStatus() != TicketStatus.IN_EVALUATION) {
            throw new IllegalStateException("O status atual do ticket não permite realizar a operação");
        }

        ticket.setStatus(TicketStatus.NEGOTIATION);
        ticket.setCurrentSector(Sector.COMMERCIAL);
        ticketRepository.save(ticket);

        List<DealProcedure> procedures = dto.procedures().stream()
                .map(p -> new DealProcedure(
                        p.name(),p.code(),
                        p.tableValue(),p.quantity(),
                        p.note()
                ))
                .toList();

        var totalValue = procedures.stream()
                .map(p -> p.tableValue().multiply(BigDecimal.valueOf(p.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Deal deal = Deal.builder()
                .ticketId(ticketId)
                .procedures(procedures)
                .createdBySector(Sector.EVALUATOR)
                .createdBy(currentUser.getId())
                .totalValue(totalValue)
                .build();

        return dealRepository.save(deal);
    }

    @Override
    @Transactional
    public Deal update(UUID dealId, DealUpdateRequestDTO dto) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal Not Found"));

        var currentUser = securityUtils.getCurrentUser();

        if (!permissionService.canAccess(
                currentUser, Resource.DEAL,
                Action.UPDATE, Sector.EVALUATOR,
                null
        )
                && !permissionService.canAccess(
                        currentUser, Resource.DEAL,
                Action.UPDATE, Sector.COMMERCIAL,
                null
        )) {
            throw new AccessDeniedException("User not have access");
        }

        if (deal.isArchived()) {
            throw new IllegalStateException("O status arquivado não permite alterações");
        }

        var proceduresBefore = deal.getProcedures();

        List<DealProcedure> procedures = dto.procedures().stream()
                .map(p -> new DealProcedure(
                        p.name(), p.code(),
                        p.tableValue(),p.quantity(),
                        p.note()
                ))
                .toList();

        var newValue = procedures.stream()
                .map(p -> p.tableValue().multiply(BigDecimal.valueOf(p.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        deal.setProcedures(procedures);
        deal.setTotalValue(newValue);

        Deal saved = dealRepository.save(deal);
        dealHistoryService.record(
                saved.getId(),currentUser,
                "procedures",proceduresBefore,
                saved.getProcedures()
        );

        return saved;
    }

    @Override
    @Transactional
    public Deal applyDiscount(UUID dealId, ApplyDiscountRequestDTO dto) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal Not Found"));

        var currentUser = securityUtils.getCurrentUser();

        if (!permissionService.canAccess(
                currentUser, Resource.DEAL,
                Action.UPDATE, Sector.COMMERCIAL,
                null
        )) {
            throw new AccessDeniedException("User not have access");
        }

        if (deal.isArchived()) {
            throw new IllegalStateException("O status arquivado não permite alterações");
        }

        if (dto.discountPct().compareTo(BigDecimal.ZERO) < 0 ||
                dto.discountPct().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException("Pct não deve ter valores negativos ou superiores a 100");
        }

        var discountBefore = deal.getDiscountPct();
        var finalValueBefore = deal.getFinalValue();

        deal.setDiscountPct(dto.discountPct());
        deal.setDiscountApprovedBy(currentUser.getId());

        BigDecimal factor = BigDecimal.ONE.subtract(
                dto.discountPct().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        );
        deal.setFinalValue(deal.getTotalValue().multiply(factor).setScale(2, RoundingMode.HALF_UP));

        Deal saved = dealRepository.save(deal);
        dealHistoryService.record(
                saved.getId(),
                currentUser,
                "discountPct",
                discountBefore,
                saved.getDiscountPct()
        );
        dealHistoryService.record(
                saved.getId(),
                currentUser,
                "finalValue",
                finalValueBefore,
                saved.getFinalValue()
        );

        return saved;
    }

    @Override
    @Transactional
    public Deal closeDeal(UUID dealId, String paymentMethod) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal Not Found"));

        var currentUser = securityUtils.getCurrentUser();

        if (!permissionService.canAccess(
                currentUser,Resource.DEAL,
                Action.CLOSE, Sector.COMMERCIAL,
                null)
        ) {
            throw new AccessDeniedException("User not have access");
        }

        if (deal.isArchived()) {
            throw new IllegalStateException("O status arquivado não permite alterações");
        }

        if (deal.getClosedAt() != null) {
            throw new IllegalStateException("Contrato já consta como fechado");
        }

        deal.setClosedAt(LocalDateTime.now());
        deal.setClosedBy(currentUser.getId());
        deal.setPaymentMethod(paymentMethod);

        var ticket = ticketRepository.findById(deal.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Not Found"));
        ticket.setStatus(TicketStatus.WIN);
        ticket.setClosedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        Deal saved = dealRepository.save(deal);

        dealHistoryService.record(
                saved.getId(),
                currentUser,
                "closedAt",
                null,
                saved.getClosedAt()
        );

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public DealDetailResponseDTO getDealWithHistory(UUID dealId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal Not Found"));

        return new DealDetailResponseDTO(
                dealMapper.toResponseDTO(deal),
                dealHistoryService.findByDealId(dealId).stream()
                        .map(dealMapper::toResponseDTO)
                        .toList()
        );
    }
}