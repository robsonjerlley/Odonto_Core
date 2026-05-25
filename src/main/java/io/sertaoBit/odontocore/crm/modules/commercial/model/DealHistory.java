package io.sertaoBit.odontocore.crm.modules.commercial.model;

import io.sertaoBit.odontocore.crm.core.enums.Sector;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deal_history")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = "id")
@Builder
public class DealHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID dealId;
    @Column(nullable = false)
    private UUID changedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector changedBySector;
    private String fieldChanged;
    @Column(nullable = false)
    private String valueBefore;
    @Column(nullable = false)
    private String valueAfter;
    @Column(nullable = false)
    private LocalDateTime occurredAt;
}
