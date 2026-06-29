package io.sertaoBit.odontocore.crm.modules.appointment.repository;

import io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AppointmentSpecifications {

    private AppointmentSpecifications() {}

    public static Specification<Appointment> hasStatus(AppointmentStatus status) {
        if(status == null)return(root, criteriaQuery, cb) -> cb.conjunction();

        return (root, criteriaQuery, cb
        ) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> assignedTo(UUID assignedTo) {
        if(assignedTo == null) return (root, criteriaQuery, cb) -> cb.conjunction();

        return (root, criteriaQuery, cb
        ) -> cb.equal(root.get("assignedTo"), assignedTo);
    }

    public static Specification<Appointment> scheduledBetween(LocalDateTime from, LocalDateTime to) {
        if(from == null || to == null) return (root, criteriaQuery, cb) -> cb.conjunction();

        return ((root, query, cb
        ) ->  cb.between(root.get("scheduledAt"), from, to));
    }

    public static Specification<Appointment> byScope(PermissionScope scope, User user) {
        return switch (scope) {
            case GLOBAL, SECTOR -> (root, criteriaQuery, cb) -> cb.conjunction();
            case OWN -> assignedTo(user.getId());
            case INTAKE -> throw new UnsupportedOperationException();
        };
    }


}
