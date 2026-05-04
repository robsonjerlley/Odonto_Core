package io.sertaoBit.odontocore.crm.modules.identity.service.impl;


import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IPermissionRuleRepository;
import io.sertaoBit.odontocore.crm.shared.enums.Action;
import io.sertaoBit.odontocore.crm.shared.enums.Department;
import io.sertaoBit.odontocore.crm.shared.enums.Resource;
import io.sertaoBit.odontocore.crm.shared.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl {

    private final IPermissionRuleRepository ruleRepository;


    public Boolean canAccess(
            User user,
            Resource resource,
            Action action,
            Department targetDepartment,
            UUID targetOwnerId) {

        Optional<PermissionRule> ruleOpt =
                ruleRepository.findByRoleAndResourceAndAction(user.getRole(), resource, action);

        if (ruleOpt.isEmpty() || ruleOpt.get().isAllowed()) {
            return false;
        }

        PermissionRule rule = ruleOpt.get();

        return switch (rule.getScope()) {
            case GLOBAL -> true;
            case DEPARTMENT -> user.getDepartment().equals(targetDepartment);
            case OWNER -> user.getId().equals(targetOwnerId);
        };


    }


    public List<PermissionRule> getPermission(Role role, Department department) {
        return null;
    }

    public void seedDefaultRules() {

    }

    private boolean resolveScope(
            PermissionRule rule,
            User user,
            Department targetDepartment,
            UUID targetOwnerId
    ) {
        return true;
    }
}
