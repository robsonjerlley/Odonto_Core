package io.sertaoBit.odontocore.crm.modules.identity.domain.model;

import io.sertaoBit.odontocore.crm.core.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tb_permission_rules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role", "sector", "resource", "action"})
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public class PermissionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Sector sector;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resource resource;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionScope scope;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> conditions;
    private boolean allowed;
    @CreationTimestamp
    private LocalDateTime createdAt;

}
