package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_clientes", schema = "crm_db")
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
@EqualsAndHashCode(of = "id")
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
    @CollectionTable(name = "customer_descriptions",joinColumns = @JoinColumn(name = "customer_id"))
    @Column(length = 350)
    private List<String> description;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private TicketStatus ticketStatus;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;



}
