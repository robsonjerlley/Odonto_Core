package io.sertaoBit.odontocore.crm.modules.identity.domain.model;


import io.sertaoBit.odontocore.crm.modules.clinic.domain.model.Clinic;
import io.sertaoBit.odontocore.crm.modules.identity.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_users", schema = "identity_db")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true , nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

}
