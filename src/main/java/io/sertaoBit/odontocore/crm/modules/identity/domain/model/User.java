package io.sertaoBit.odontocore.crm.modules.identity.domain.model;


import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Department;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_users", schema = "identity_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")

public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false, unique = true)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean active;
    private UUID createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
