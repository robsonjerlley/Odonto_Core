package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Sector.ATTENDANT;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.LEADS;

public final class ContactLogSpecifications {

    private ContactLogSpecifications() {
    }

    public static Specification<ContactLog> byTicketId(UUID ticketId) {
        if (ticketId == null) return null;

        return (root, query, cb
        ) -> cb.equal(root.get("ticketId"), ticketId);

    }

    public static Specification<ContactLog> hasTicketInSectors(List<Sector> sectors) {
        if (sectors == null || sectors.isEmpty()) return null;

        return (root, query, cb
        ) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<LeadTicket> ticket = sub.from(LeadTicket.class);
            sub.select(ticket.get("id"));
            sub.where(
                    cb.equal(ticket.get("id"), root.get("ticketId")),
                    ticket.get("currentSector").in(sectors)
            );

            return cb.exists(sub);
        };
    }

    public static Specification<ContactLog> hasTicketInSector(Sector sector) {
        return sector == null ? null : hasTicketInSectors(List.of(sector));
    }

    public static Specification<ContactLog> createdBy(UUID userId) {
        if (userId == null) return null;

        return (root, query, cb
        ) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<ContactLog> byScope(PermissionScope scope, User user) {
        return switch (scope) {
            case GLOBAL -> ((root, query, cb) -> cb.conjunction() );
            case OWN -> createdBy(user.getId());
            case SECTOR -> hasTicketInSector(user.getSector());
            case INTAKE -> hasTicketInSectors(List.of(LEADS, ATTENDANT));
        };
    }
}
