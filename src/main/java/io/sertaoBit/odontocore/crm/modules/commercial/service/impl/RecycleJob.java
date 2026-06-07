package io.sertaoBit.odontocore.crm.modules.commercial.service.impl;

import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import io.sertaoBit.odontocore.crm.modules.commercial.model.RecycleConfig;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.RecycleConfigRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static io.sertaoBit.odontocore.crm.core.enums.Sector.LEADS;
import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.*;

@Component
public class RecycleJob {

    private final LeadTicketRepository leadTicketRepository;
    private final RecycleConfigRepository recycleConfigRepository;
    private final DealRepository dealRepository;

    @Lazy
    @Autowired
    private RecycleJob self;

    public RecycleJob(
            LeadTicketRepository leadTicketRepository,
            RecycleConfigRepository recycleConfigRepository,
            DealRepository dealRepository
    ) {
        this.leadTicketRepository = leadTicketRepository;
        this.recycleConfigRepository = recycleConfigRepository;
        this.dealRepository = dealRepository;
    }


    @Scheduled(cron = "0 0 2 * * *", zone = "America/Sao_Paulo")
    public void runNightly() {
        List<LeadTicket> leadTickets = leadTicketRepository.findByStatusAndPendingAtBefore(
                PENDING, LocalDateTime.now()
        );

        for (LeadTicket leadTicket : leadTickets) {

            Optional<RecycleConfig> recycleConfig = recycleConfigRepository
                    .findBySectorAndActiveTrue(leadTicket.getCurrentSector())
                    .or(recycleConfigRepository::findBySectorIsNullAndActiveTrue);

            if (recycleConfig.isEmpty()) {
                continue;
            }

            var days = ChronoUnit.DAYS.between(leadTicket.getPendingAt(), LocalDateTime.now());

            if (days >= recycleConfig.get().getAfterDays()) {
                self.processTicket(leadTicket);
            }
        }


    }

    @Transactional
    void processTicket(LeadTicket ticket) {
        Optional<Deal> dealOpt = dealRepository.findByTicketIdAndArchivedFalse(ticket.getId());

        if (dealOpt.isPresent()) {
            dealOpt.get().setArchived(true);
            dealRepository.save(dealOpt.get());
        }

        ticket.setStatus(RECYCLED);
        ticket.setRecycledAt(LocalDateTime.now());
        leadTicketRepository.save(ticket);

        LeadTicket leadTicket = new LeadTicket();
        leadTicket.setCustomerId(ticket.getCustomerId());
        leadTicket.setStatus(NEW);
        leadTicket.setCurrentSector(LEADS);
        leadTicket.setCreatedBy(ticket.getCreatedBy());
        leadTicket.setPreviousTicketId(ticket.getId());
        leadTicketRepository.save(leadTicket);

    }


}
