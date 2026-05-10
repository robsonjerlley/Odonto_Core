package io.sertaoBit.odontocore.crm.modules.commercial.repository;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.commercial.model.BonusConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BonusConfigRepository extends JpaRepository<BonusConfig, UUID> {

    Optional<BonusConfig> findByRoleAndSectorAndPeriodRef(Role role, Sector sector, String periodRef);

    List<BonusConfig> findByRoleAndSector(Role role, Sector sector);

}
