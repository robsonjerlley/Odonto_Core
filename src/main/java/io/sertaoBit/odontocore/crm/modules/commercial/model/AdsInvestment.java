package io.sertaoBit.odontocore.crm.modules.commercial.model;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ads_investments", schema = "crm_db")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class AdsInvestment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdsChannel channel;
    private String campaign;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    @Column(nullable = false)
    private UUID registeredBy;
    @CreationTimestamp
    private LocalDateTime createdAt;

}
