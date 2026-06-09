package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Sector.ATTENDANT;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.LEADS;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> hasPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> cb.equal(root.get("phone"), phone);
    }

    public static Specification<Customer> hasName(String name) {
        if (name == null || name.isBlank()) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> cb.equal(root.get("name"), name);
    }

    public static Specification<Customer> hasAdsChannel(AdsChannel adsChannel) {
        if (adsChannel == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> cb.equal(root.get("adsChannel"), adsChannel);
    }


    public static Specification<Customer> hasTicketInSectors(List<Sector> sectors) {
        if (sectors == null || sectors.isEmpty()) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<LeadTicket> ticket = sub.from(LeadTicket.class);
            sub.select(ticket.get("id"));
            sub.where(
                    cb.equal(ticket.get("customerId"), root.get("id")),
                    ticket.get("currentSector").in(sectors)
            );
            return cb.exists(sub);
        };
    }

    public static Specification<Customer> hasTicketInSector(Sector sector) {
        if (sector == null) return (root, query, cb) -> cb.conjunction();
        return hasTicketInSectors(List.of(sector));
    }

    public static Specification<Customer> createdBy(UUID userId) {
        if (userId == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb
        ) -> cb.equal(root.get("createdBy"), userId);
    }


    public static Specification<Customer> byScope(PermissionScope scope, User user) {
        return switch (scope) {
            case GLOBAL -> ((root, query, cb) -> cb.conjunction());
            case OWN -> createdBy(user.getId());
            case SECTOR -> hasTicketInSector(user.getSector());
            case INTAKE -> hasTicketInSectors(List.of(LEADS, ATTENDANT));
        };
    }
}
