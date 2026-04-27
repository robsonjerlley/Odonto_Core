package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactOutcome;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
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
@EqualsAndHashCode(of = "id")
public class ContactLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @Nullable
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY)
    private User contactBy;
    @Enumerated(EnumType.STRING)
    private ContactChannel contactChannel;
    @Column(length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    private ContactOutcome contactOutcome;
    @CreationTimestamp
    private LocalDateTime contactDate;
    private LocalDate nextFollowUp;

    //CAMPOS PARA TESTES, AINDA NÃO DEFINIDOS COMO PERMANENTES
    private BigDecimal investmentAmount;
    private BigDecimal conversionValue;

}
