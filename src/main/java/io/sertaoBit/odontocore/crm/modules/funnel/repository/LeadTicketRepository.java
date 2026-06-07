package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeadTicketRepository extends JpaRepository<LeadTicket, UUID>, JpaSpecificationExecutor<LeadTicket> {

    Page<LeadTicket> findByCustomerId(UUID customerId, Pageable pageable);

    Page<LeadTicket> findByAssignedTo(UUID userId, Pageable pageable);

    List<LeadTicket> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    Page<LeadTicket> findByStatus(TicketStatus status, Pageable pageable);

    List<LeadTicket> findByStatusAndPendingAtBefore(TicketStatus status, LocalDateTime date);

    List<LeadTicket> findByProcedurePerformedAtBetween(LocalDateTime from, LocalDateTime to);

    List<LeadTicket> findByCustomerIdInAndStatusAndClosedAtBetween(
            List<UUID> customerIds,
            TicketStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

}
