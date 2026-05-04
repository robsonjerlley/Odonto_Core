package io.sertaoBit.odontocore.crm.modules.identity.repository;

import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.shared.enums.Action;
import io.sertaoBit.odontocore.crm.shared.enums.Department;
import io.sertaoBit.odontocore.crm.shared.enums.Resource;
import io.sertaoBit.odontocore.crm.shared.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPermissionRuleRepository extends JpaRepository<PermissionRule, UUID> {


    Optional<PermissionRule> findByRoleAndResourceAndAction(
            Role role, Resource resource, Action action
    );


    Optional<PermissionRule> findByRoleAndDepartmentAndResourceAndAction(
            Role role, Department department, Resource resource, Action action
    );

    List<PermissionRule> findAllByRole(Role role);
}
