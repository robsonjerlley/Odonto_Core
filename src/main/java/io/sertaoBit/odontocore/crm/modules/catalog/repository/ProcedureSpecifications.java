package io.sertaoBit.odontocore.crm.modules.catalog.repository;

import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.modules.catalog.domain.model.Procedure;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ProcedureSpecifications {

    private ProcedureSpecifications() {
    }

    public static Specification<Procedure> hasName(String name) {
        if (name == null || name.isBlank()) return (root, criteriaQuery, cb) -> cb.conjunction();

        return (root, query, cb)
                -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }


    public static Specification<Procedure> hasCode(String code) {
        if (code == null || code.isBlank()) return (root, criteriaQuery, cb) -> cb.conjunction();
        return (root, query, cb)
                -> cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    public static Specification<Procedure>createdBy(UUID userId) {
        if (userId == null) return (root, criteriaQuery, cb) -> cb.conjunction();
        return (root, query, cb) -> cb.equal(root.get("createdBy"), userId);
    }

    public static Specification<Procedure> isActive() {
        return (root, criteriaQuery, cb)
                -> cb.isTrue(root.get("active"));
    }

    public static Specification<Procedure> byScope(PermissionScope scope, User user) {
        return switch (scope) {
            case GLOBAL -> (root, criteriaQuery, cb) -> cb.conjunction();
            case OWN -> createdBy(user.getId());
            case SECTOR, INTAKE, PIPELINE -> throw new UnsupportedOperationException("Not supported yet.");
        };
    }
}
