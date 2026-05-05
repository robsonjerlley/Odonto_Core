package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IDealRepository extends JpaRepository<Deal, UUID> {

    List<Deal> findByDealStatus(DealStatus dealStatus);

    List<Deal> findByCustomerId(UUID customerId);

    List<Deal> findByClosedDateBetween(LocalDateTime start, LocalDateTime end);

    List<Deal> findByResponsibleId(UUID userId);
}
