package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_clientes", schema = "crm_db")
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
@EqualsAndHashCode(of = "id")
public class Costumer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    private String name;
    @Column(unique = true)
    private String telephone;
    private String city;
    private String address;
    private String description;
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private TicketStatus ticket;

    @OneToOne(fetch = FetchType.LAZY)
    private User user;



}
