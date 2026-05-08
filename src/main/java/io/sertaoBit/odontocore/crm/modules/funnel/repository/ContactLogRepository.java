package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactLogRepository extends JpaRepository<ContactLog, UUID> {

    List<ContactLog> findByTicketId(UUID ticketId);

}
