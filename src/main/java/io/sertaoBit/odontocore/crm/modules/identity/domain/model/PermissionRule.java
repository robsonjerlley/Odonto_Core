package io.sertaoBit.odontocore.crm.modules.identity.domain.model;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Department;


import io.sertaoBit.odontocore.crm.core.enums.Action;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Resource;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_regras_de_premissoes", schema = "identity_db")
@NoArgsConstructor @AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public class PermissionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resource resource;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionScope scope;
    private boolean allowed;
    @CreationTimestamp
    private LocalDateTime created;

}
