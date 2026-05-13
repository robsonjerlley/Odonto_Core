package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeadTicketRepository extends JpaRepository<LeadTicket, UUID> {

    List<LeadTicket> findByCustomerId(UUID customerId);

    List<LeadTicket> findByAssignedTo(UUID userId);

    List<LeadTicket> findByCurrentSector(Sector sector);

    List<LeadTicket> findByCurrentSectorAndAssignedTo(Sector sector, UUID userId);

    List<LeadTicket> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<LeadTicket> findByStatus(TicketStatus status);

    List<LeadTicket> findByStatusAndPendingAtBefore(TicketStatus status, LocalDateTime date);


}
