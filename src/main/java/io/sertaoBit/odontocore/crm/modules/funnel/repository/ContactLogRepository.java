package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContactLogRepository extends JpaRepository<ContactLog, UUID> {


    Page<ContactLog> findByTicketId(UUID ticketId, Pageable pageable);

}
