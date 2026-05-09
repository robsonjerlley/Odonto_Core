package io.sertaoBit.odontocore.crm.modules.commercial.repository;

import io.sertaoBit.odontocore.crm.modules.commercial.model.DealHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DealHistoryRepository extends JpaRepository<DealHistory, UUID> {

    List<DealHistory> findByDealIdOrderByOccurredAt(UUID dealId);
}
