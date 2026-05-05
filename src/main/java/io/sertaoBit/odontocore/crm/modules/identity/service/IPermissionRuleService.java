package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.core.enums.*;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IPermissionRuleService {

    Boolean canAccess(
            User user, Resource resource,
            Action action, Sector targetSector,
            UUID targetOwnerId
    );

    List<PermissionRule> getPermission(Role role, Sector sector);

    void seedDefaultRules();
}
