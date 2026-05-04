package io.sertaoBit.odontocore.crm.modules.identity.service;


import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IPermissionRuleRepository;
import io.sertaoBit.odontocore.crm.shared.enums.Action;
import io.sertaoBit.odontocore.crm.shared.enums.Department;
import io.sertaoBit.odontocore.crm.shared.enums.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final IPermissionRuleRepository ruleRepository;


    public Boolean canAccess(
            User user,
            Resource resource,
            Action action,
            Department department,
            UUID targetOwnerId) {

        Optional<PermissionRule> ruleOpt =
                ruleRepository.findByRoleAndResourceAndAction(user.getRole(), resource, action);

        if (ruleOpt.isEmpty() || ruleOpt.get().isAllowed()) {
            return false;
        }

        PermissionRule rule = ruleOpt.get();

        return switch (rule.getScope()) {
            case GLOBAL -> true;
            case DEPARTMENT -> user.getDepartment().equals(department);
            case OWNER -> user.getId().equals(targetOwnerId);
        };


    }
}
