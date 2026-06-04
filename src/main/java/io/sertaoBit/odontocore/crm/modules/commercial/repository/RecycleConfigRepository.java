package io.sertaoBit.odontocore.crm.modules.commercial.repository;

import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.model.RecycleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecycleConfigRepository extends JpaRepository<RecycleConfig, UUID> {

    Optional<RecycleConfig> findBySectorAndActiveTrue(Sector sector);

    Optional<RecycleConfig> findBySectorIsNullAndActiveTrue();

  Optional<RecycleConfig> findFirstByActiveTrueOrderByCreatedAtDesc();

}
