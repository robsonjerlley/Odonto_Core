package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Sector.ATTENDANT;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.EVALUATOR;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.LEADS;

public final class LeadTicketSpecifications {

    private LeadTicketSpecifications() {
    }

    public static Specification<LeadTicket> hasStatus(TicketStatus status) {
        if (status == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> cb.equal(root.get("status"), status);
    }

    public static Specification<LeadTicket> hasCustomerId(UUID customerId) {
        if (customerId == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> cb.equal(root.get("customerId"), customerId);
    }


    public static Specification<LeadTicket> assignedTo(UUID assignedTo) {
        if (assignedTo == null) return (root, query, cb) -> cb.conjunction();
        return ((root, query, cb
        ) -> cb.equal(root.get("assignedTo"), assignedTo));
    }

    public static Specification<LeadTicket> createdBy(UUID userId) {
        if (userId == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> cb.equal(root.get("createdBy"), userId);
    }

    public static Specification<LeadTicket> currentSector(Sector sector) {
        if (sector == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> cb.equal(root.get("currentSector"), sector);
    }

    public static Specification<LeadTicket> currentSectorIn(List<Sector> sectors) {
        if (sectors == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> root.get("currentSector").in(sectors);
    }

    public static Specification<LeadTicket> byScope(PermissionScope scope, User user) {
        return switch (scope) {
            case GLOBAL -> ((root, query, cb) -> cb.conjunction());
            case SECTOR -> currentSector(user.getSector());
            case INTAKE -> currentSectorIn(List.of(LEADS, ATTENDANT));
            case PIPELINE -> currentSectorIn(List.of(LEADS, ATTENDANT, EVALUATOR));
            case OWN -> createdBy(user.getId());
        };
    }

}
