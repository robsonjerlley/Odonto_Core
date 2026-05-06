package io.sertaoBit.odontocore.crm.modules.identity.service;


import io.sertaoBit.odontocore.crm.core.enums.*;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.PermissionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRuleRepository ruleRepository;


    public Boolean canAccess(
            User user,
            Resource resource,
            Action action,
            Sector targetSector,
            UUID targetOwnerId) {

        Optional<PermissionRule> ruleOpt =
                ruleRepository.findByRoleAndResourceAndAction(user.getRole(), resource, action);

        if (ruleOpt.isEmpty() || !ruleOpt.get().isAllowed()) {
            return false;
        }

        PermissionRule rule = ruleOpt.get();

        return switch (rule.getScope()) {
            case GLOBAL -> true;
            case SECTOR -> user.getSector().equals(targetSector);
            case OWNER -> user.getId().equals(targetOwnerId);
        };


    }


    public List<PermissionRule> getPermission(Role role, Sector sector, Action action) {

        return null;

    }

    public void seedDefaultRules() {

        return;



    }

}
