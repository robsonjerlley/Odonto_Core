package io.sertaoBit.odontocore.crm.modules.commercial.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
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
import io.sertaoBit.odontocore.crm.modules.commercial.service.DealService;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;


@Service
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadTicketRepository ticketRepository;
    private final SecurityUtils securityUtils;
    private final DealMapper dealMapper;

    public DealServiceImpl(
            DealRepository dealRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository, LeadTicketRepository ticketRepository, SecurityUtils securityUtils,
            DealMapper dealMapper
    ) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.ticketRepository = ticketRepository;
        this.securityUtils = securityUtils;
        this.dealMapper = dealMapper;
    }


    @Override
    @Transactional
    public Deal create(UUID ticketId, DealCreateRequestDTO dto) {
        LeadTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Not Found"));
        var currentUser = securityUtils.getCurrentUser();

        if(ticket.getStatus() != TicketStatus.NEGOTIATION) {
            throw new IllegalStateException("O status atual do ticket não permite realizar a operação");
        }

        List<DealProcedure> procedure = dto.procedures().stream()
                .map(p ->
                        new DealProcedure(p.name(), p.code()
                                , p.tableValue(), p.quantity(), p.note())
                ).toList();


        Deal deal = Deal.builder()
                .ticketId(ticketId)
                .procedures(procedure)
                .createdBySector(currentUser.getSector())
                .createdBy(currentUser.getId())
                .build();

        return dealRepository.save(deal);
    }

    @Override
    @Transactional
    public Deal update(UUID dealId, DealUpdateRequestDTO dto) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal Not Found"));

            if(deal.isArchived()){
                throw new IllegalStateException("O status arquivado não permite alterações");
            }
        List<DealProcedure> procedure = dto.procedures().stream()
                .map(p ->
                        new DealProcedure(p.name(), p.code()
                                , p.tableValue(), p.quantity(), p.note())
                ).toList();

       var newValue = procedure.stream()
               .map(p -> p.tableValue().multiply(BigDecimal.valueOf(p.quantity())))
               .reduce(BigDecimal.ZERO, BigDecimal::add);

        deal.setProcedures(procedure);
        deal.setTotalValue(newValue);

        return dealRepository.save(deal);
    }

    @Override
    public Deal applyDiscount(UUID dealId, ApplyDiscountRequestDTO dto) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal Not Found"));

        var currentUser = securityUtils.getCurrentUser();

        if(deal.isArchived()){
            throw new IllegalStateException("O status arquivado não permite alterações");
        }

        if(dto.discountPct().compareTo(BigDecimal.ZERO) < 0  ||
           dto.discountPct().compareTo(BigDecimal.valueOf(100)) > 0)
        {
            throw new IllegalStateException("Pct não deve ter valores negativos ou superiores a 100");
        }

        deal.setDiscountPct(dto.discountPct());
        deal.setDiscountApprovedBy(currentUser.getId());

        BigDecimal factor = BigDecimal.ONE.subtract(
                dto.discountPct().divide(BigDecimal.valueOf(100))
        );

        deal.setFinalValue(deal.getTotalValue()
                .multiply(factor).setScale(2,
                        RoundingMode.HALF_UP)
        );

        return dealRepository.save(deal);
    }

    @Override
    public Deal closeDeal(UUID dealId, String paymentMethod) {
        return null;
    }

    @Override
    public DealDetailResponseDTO getDealWithHistory(UUID dealId) {
        return null;
    }
}
