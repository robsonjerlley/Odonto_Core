package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.shared.enums.Action;
import io.sertaoBit.odontocore.crm.shared.enums.Department;
import io.sertaoBit.odontocore.crm.shared.enums.Resource;
import io.sertaoBit.odontocore.crm.shared.enums.Role;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IPermissionRuleService {

    Boolean canAccess(
            User user,Resource resource,
            Action action,Department targetDepartment,
            UUID targetOwnerId
    );

    List<PermissionRule> getPermission(Role role, Department department);

    void seedDefaultRules();
}
