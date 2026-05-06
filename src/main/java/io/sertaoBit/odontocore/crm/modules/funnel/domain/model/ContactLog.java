package io.sertaoBit.odontocore.crm.modules.funnel.domain.model;

import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_logs_de_contato", schema = "crm_db")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ContactLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private UUID ticketId;
    @ManyToOne(fetch = FetchType.LAZY)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @NotNull
    private ContactChannel contactChannel;
    @Column(length = 500)
    @NotNull
    private String description;
    @Enumerated(EnumType.STRING)
    private TicketStatus statusBefore;
    @Enumerated(EnumType.STRING)
    private TicketStatus statusAfter;
    @NotNull
    private LocalDateTime occurredAt;
    @CreationTimestamp
    private LocalDateTime createdAt;


}
