package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_metricas_de_vendas", schema = "crm_bd")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SalesMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private LocalDate period;
    @Enumerated(EnumType.STRING)
    private ContactChannel contactChannel;
    @ManyToOne(fetch = FetchType.LAZY)
    private Department department;
    @ManyToOne(fetch = FetchType.LAZY)
    private User userId;
    @Column(nullable = false)
    private Integer totalContact;
    @Column(nullable = false)
    private Integer successfulContact;
    @Column(nullable = false)
    private Integer failedContact;
    @Column(nullable = false)
    private Integer pendingFollowUp;
    @Column(nullable = false)
    private BigDecimal successRate;
    @Column(nullable = false)
    private BigDecimal conversionRate;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
