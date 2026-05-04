package io.sertaoBit.odontocore.crm.modules.identity.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Department;


import io.sertaoBit.odontocore.crm.shared.enums.Action;
import io.sertaoBit.odontocore.crm.shared.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.shared.enums.Resource;
import io.sertaoBit.odontocore.crm.shared.enums.Role;
import jakarta.persistence.Entity;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class PermissionRule {

    private UUID id;
    private Role role;
    private Department department;
    private Resource resource;
    private Action action;
    private PermissionScope scope;
    private boolean allowed;
    private LocalDateTime created;

}
