package io.sertaoBit.odontocore.crm.modules.funnel.domain.model;

import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_logs", schema = "crm_db")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ContactLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID ticketId;
    @Column(nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactChannel channel;
    @Column(length = 500, nullable = false)
    private String note;
    @Enumerated(EnumType.STRING)
    private TicketStatus statusBefore;
    @Enumerated(EnumType.STRING)
    private TicketStatus statusAfter;
    @Column(nullable = false)
    private LocalDateTime occurredAt;
    @CreationTimestamp
    private LocalDateTime createdAt;


}
