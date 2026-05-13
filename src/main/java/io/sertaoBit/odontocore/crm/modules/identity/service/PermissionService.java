package io.sertaoBit.odontocore.crm.modules.identity.service;


import io.sertaoBit.odontocore.crm.core.enums.Action;
import io.sertaoBit.odontocore.crm.core.enums.Resource;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.PermissionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
                ruleRepository.findByRoleAndSectorAndResourceAndAction(
                        user.getRole(), user.getSector(), resource, action);

        if (ruleOpt.isEmpty()) {
            ruleOpt = ruleRepository.findByRoleAndResourceAndAction(
                    user.getRole(), resource, action);
        }

        if (ruleOpt.isEmpty() || !ruleOpt.get().isAllowed()) {
            return false;
        }

        return resolveScope(ruleOpt.get(), user, targetSector, targetOwnerId);
    }


    public void checkOrThrow(
            User user,
            Resource resource,
            Action action,
            Sector targetSector,
            UUID targetOwnerId
    ) {
        if (!canAccess(user, resource, action, targetSector, targetOwnerId)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public List<PermissionRule> getPermission(Role role) {
        return ruleRepository.findAllByRole(role);

    }



    private Boolean resolveScope(
            PermissionRule rule,
            User user,
            Sector targetSector,
            UUID targetOwnerId
    ) {

        return switch (rule.getScope()) {
            case GLOBAL -> true;
            case SECTOR -> user.getSector().equals(targetSector);
            case OWN -> user.getId().equals(targetOwnerId);
        };

    }

}
