package io.sertaoBit.odontocore.crm.modules.identity.domain.model;


import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Department;
import io.sertaoBit.odontocore.crm.modules.identity.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_users", schema = "identity_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false, unique = true)
    private String password;
    @ManyToOne(fetch = FetchType.LAZY)
    private Department department;
    @Enumerated(EnumType.STRING)
    private Role role;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
