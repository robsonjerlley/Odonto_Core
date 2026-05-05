package io.sertaoBit.odontocore.crm.modules.funnel.domain.model;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="tb_tickets", schema = "crm_db")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    @Enumerated(EnumType.STRING)
    private TicketStatus ticketStatus;
    @Enumerated(EnumType.STRING)
    private Priority priority;
    @ManyToOne(fetch = FetchType.LAZY)
    private User assigneToUser;
    @Column(length = 350 , nullable = false)
    private String description;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;



}
