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
@Table(name = "tb_lead_tickets", schema = "crm_db")
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
    @ManyToOne(fetch = FetchType.LAZY)
    private UUID customerId;
    @Enumerated(EnumType.STRING)
    private TicketStatus ticketStatus;
    @Enumerated(EnumType.STRING)
    private Sector currentSector;
    @ManyToOne(fetch = FetchType.LAZY)
    private UUID assigneToUser;
    @Column(length = 350, nullable = false)
    private String description;
    private LocalDateTime scheduledAt;
    private LocalDateTime pendingAt;
    @CreationTimestamp
    private LocalDateTime closedAt;
    private UUID createdByUser;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
