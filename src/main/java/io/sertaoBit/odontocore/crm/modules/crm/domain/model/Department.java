package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.DepartmentName;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
@Entity
@Table(name = "tb_departamentos", schema = "identity_db" ,
uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of ="id")
@ToString(exclude = "permissions")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private DepartmentName name;
    @Column(length = 350 , nullable = false)
    private String description;
    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "permission")
    private Set<String> permissions;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
