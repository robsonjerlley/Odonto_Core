package io.sertaoBit.odontocore.crm.modules.crm.repository;

import io.sertaoBit.odontocore.crm.modules.crm.domain.model.SalesMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface ISalesMetricsRepository extends JpaRepository<SalesMetrics, UUID> {
}
