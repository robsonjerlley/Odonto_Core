package io.sertaoBit.odontocore.crm.modules.financial.repository;

import io.sertaoBit.odontocore.crm.core.enums.PaymentStatus;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class InstallmentSpecifications {

    private InstallmentSpecifications() {
    }


    public static Specification<Installment> hasStatus(PaymentStatus status) {
        if (status == null) return (root, criteriaQuery, cb) -> cb.conjunction();
        return (root, criteriaQuery, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Installment> byCustomer(UUID customerId) {
        if (customerId == null) return (root, criteriaQuery, cb) -> cb.conjunction();
        return (root, criteriaQuery, cb) -> cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<Installment> byDeal(UUID dealId) {
        if (dealId == null) return (root, criteriaQuery, cb) -> cb.conjunction();
        return (root, criteriaQuery, cb) -> cb.equal(root.get("dealId"), dealId);
    }

    public static Specification<Installment> dueBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) return (root, criteriaQuery, cb) -> cb.conjunction();
        return (root, criteriaQuery, cb) -> cb.between(root.get("dueDate"), from, to);
    }

    /** Atrasada = ainda EXPECTED e vencida antes de hoje (mesma regra do InstallmentMapper.isOverdue). */
    public static Specification<Installment> overdue() {
        return (root, criteriaQuery, cb) -> cb.and(
                cb.equal(root.get("status"), PaymentStatus.EXPECTED),
                cb.lessThan(root.get("dueDate"), LocalDate.now())
        );
    }

    public static Specification<Installment> byScope(PermissionScope scope, User user) {

        return switch (scope) {
            case GLOBAL, SECTOR -> (root, criteriaQuery, cb) -> cb.conjunction();
            case OWN, INTAKE -> throw new UnsupportedOperationException();

        };
    }

}
