package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.DealStatus;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
@Entity
@Table(name = "tb_acordos" , schema = "crm_db")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    @Enumerated(EnumType.STRING)
    private DealStatus dealStatus;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> procedures;
    @ManyToOne(fetch = FetchType.LAZY)
    private User closedBy;
    @Column(length = 350)
    private String description;
    @CreationTimestamp
    private LocalDateTime closedDate;
    @UpdateTimestamp
    private LocalDateTime targetDate;

}
