package io.sertaoBit.odontocore.crm.modules.commercial.repository;

import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DealRepository extends JpaRepository<Deal, UUID> {

    Optional<Deal> findByTicketId(UUID ticketId);

    Optional<Deal> findByTicketIdAndArchivedFalse(UUID ticketId);

    List<Deal> findByCreatedByAndClosedAtBetween(UUID userId, LocalDateTime from, LocalDateTime to);

    List<Deal> findByClosedByAndClosedAtBetween(UUID userId, LocalDateTime from, LocalDateTime to);

    List<Deal> findByTicketIdIn(List<UUID> ticketIds);

}
