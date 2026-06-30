package io.sertaoBit.odontocore.crm.modules.identity.repository;

import io.sertaoBit.odontocore.crm.core.enums.*;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRuleRepository extends JpaRepository<PermissionRule, UUID> {


    Optional<PermissionRule> findByRoleAndResourceAndAction(
            Role role, Resource resource, Action action
    );


    Optional<PermissionRule> findByRoleAndSectorAndResourceAndAction(
            Role role, Sector sector, Resource resource, Action action
    );

}
