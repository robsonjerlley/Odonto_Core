package io.sertaoBit.odontocore.crm.modules.funnel.domain.model;

import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_logs_de_contato", schema = "crm_db", indexes = {
        @Index(name = "idx_customer_date", columnList = "customer_id, contact_date"),
        @Index(name = "idx_channel_outcome", columnList = "contact_channel, contact_out_come")
})
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
    private ContactChannel  contactChannel;
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
