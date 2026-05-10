package io.sertaoBit.odontocore.crm.modules.commercial.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.mapper.DealMapper;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.service.DealService;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadTicketRepository ticketRepository;
    private final SecurityUtils  securityUtils;
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

        Deal deal = Deal.builder()
                .ticketId(ticketId)

                .build();

        return null;
    }

    @Override
    public Deal update(UUID dealId, DealUpdateRequestDTO dto) {
        return null;
    }

    @Override
    public Deal applyDiscount(UUID dealId, BigDecimal pct) {
        return null;
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
