package io.sertaoBit.odontocore.crm.modules.funnel.domain.model;


import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Builder
public class LeadTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID customerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector currentSector;
    private UUID assignedTo;
    private LocalDateTime scheduledAt;
    private LocalDateTime pendingAt;
    private LocalDateTime closedAt;
    @Column(nullable = false)
    private UUID createdBy;
    private UUID previousTicketId;
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private LocalDateTime recycledAt;

}
