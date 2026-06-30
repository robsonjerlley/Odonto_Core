package io.sertaoBit.odontocore.crm.modules.commercial.model;

import io.sertaoBit.odontocore.crm.core.enums.PaymentMethod;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "deals", schema = "crm_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Builder
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID ticketId;
    @Enumerated(EnumType.STRING)
    private Sector createdBySector;
    @Column(nullable = false)
    private UUID createdBy;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<DealProcedure> procedures;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValue;
    @Column(precision = 15, scale = 2)
    private BigDecimal discountPct;
    private UUID discountApprovedBy;
    @Column(precision = 15, scale = 2)
    private BigDecimal finalValue;
    private Integer installmentCount;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private UUID closedBy;
    private LocalDateTime closedAt;
    private boolean archived;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
