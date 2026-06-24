package io.sertaoBit.odontocore.crm.modules.catalog.domain.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "procedures", schema = "crm_db")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @TenantId
    @Column(name = "clinic_id", nullable = false, updatable = false)
    private UUID clinicId;
    @Column(nullable = false)
    private String name;
    private String code;
    @Column(nullable = false)
    private boolean active;
    private int estimatedDuration;
    @Column(nullable = false)
    private BigDecimal defaultPrice;
    @Column(nullable = false)
    private UUID createdBy;
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
