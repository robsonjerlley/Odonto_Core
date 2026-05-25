package io.sertaoBit.odontocore.crm.modules.commercial.model;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bonus_config")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Builder
public class BonusConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector sector;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false)
    private String metricKey;
    @Column(precision = 15, scale = 2)
    private BigDecimal bonusPct;
    @Column(precision = 15, scale = 2)
    private BigDecimal targetValue;
    @Builder.Default
    private boolean active = true;
    @Column(nullable = false)
    private UUID configuredBy;
    private String periodRef;
    @CreationTimestamp
    private LocalDateTime createdAt;

}
