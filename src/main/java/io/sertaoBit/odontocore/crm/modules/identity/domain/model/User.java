package io.sertaoBit.odontocore.crm.modules.identity.domain.model;


import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(unique = true, nullable = false)
    @NotNull
    private String username;
    @Column(nullable = false)
    @NotNull
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private Sector sector;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private Role role;
    @Column(columnDefinition = "boolean default true")
    private boolean active;
    private UUID createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
