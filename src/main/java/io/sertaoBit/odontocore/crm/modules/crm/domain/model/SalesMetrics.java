package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="tb_metricas_de_vendas" , schema = "crm_bd")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
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
    private Integer totalContact;
    private Integer successfulContact;
    private Integer failedContact;
    private Integer pendingFollowUp;
    private BigDecimal successRate;
    private BigDecimal conversionRate;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
