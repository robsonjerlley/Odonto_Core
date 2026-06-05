package io.sertaoBit.odontocore.crm.modules.identity.service;


import io.sertaoBit.odontocore.crm.core.enums.Action;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Resource;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.PermissionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

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
            UUID targetOwnerId
    ) {

        return getScope(user, resource, action)
                .map(scope -> resolveScope(scope, user, targetSector, targetOwnerId))
                .orElse(false);

    }


    public Optional<PermissionScope> getScope(User user, Resource resource, Action action) {
        Optional<PermissionRule> ruleOpt =
                ruleRepository.findByRoleAndSectorAndResourceAndAction(
                        user.getRole(), user.getSector(), resource, action);

        if (ruleOpt.isEmpty()) {
            ruleOpt = ruleRepository.findByRoleAndResourceAndAction(
                    user.getRole(), resource, action);
        }

        if (ruleOpt.isEmpty() || !ruleOpt.get().isAllowed()) {
            return Optional.empty();
        }
        return ruleOpt.map(PermissionRule::getScope);
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


    //Um usuário de setor de captação (`LEADS` ou `ATTENDANT`) pode acessar recursos que também estão em um setor de captação (`LEADS` ou `ATTENDANT`)
    private boolean isIntakeSector(Sector sector) {
        return sector == Sector.LEADS || sector == Sector.ATTENDANT;
    }


    private Boolean resolveScope(
            PermissionScope scope,
            User user,
            Sector targetSector,
            UUID targetOwnerId
    ) {

        return switch (scope) {
            case GLOBAL -> true;
            case SECTOR -> user.getSector().equals(targetSector);
            case OWN -> user.getId().equals(targetOwnerId);
            case INTAKE -> isIntakeSector(user.getSector()) && isIntakeSector(targetSector);
        };

    }

}

