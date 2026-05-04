package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_clientes", schema = "crm_db" , indexes = {
        @Index(name = "idx_cpf" , columnList = "cpf"),
        @Index(name = "idx_sector_status", columnList = "sector_id, ticket_status")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    @Column(unique = true)
    @CPF
    private String cpf;
    @Column(unique = true)
    private String telephone;
    private String city;
    private String address;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "customer_descriptions"
            ,joinColumns = @JoinColumn(name = "customer_id"))
    @Column(length = 500)
    private List<String> descriptions;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Enumerated(EnumType.STRING)
    private TicketStatus ticketStatus;
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

}
