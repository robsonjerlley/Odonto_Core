package io.sertaoBit.odontocore.crm.modules.commercial.model;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
@Entity
@Table(name = "tb_acordos" , schema = "crm_db" , indexes = {
        @Index(name = "idx_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_closed_date" , columnList = "closed_date")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Builder
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    @Enumerated(EnumType.STRING)
    private DealStatus dealStatus;
    @ElementCollection(fetch = FetchType.LAZY)
    private Set<String> procedures;
    @Column(nullable = false)
    private BigDecimal negotiationValue;
    @ManyToOne(fetch = FetchType.LAZY)
    private User closedBy;
    @Column(length = 350)
    private String description;
    @CreationTimestamp
    private LocalDateTime closedDate;

    private LocalDate targetDate;

}
