package io.sertaoBit.odontocore.crm.modules.crm.domain.model;

import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="tb_logs_de_contato", schema = "crm_db")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of= "id")
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
    @Column(length = 500)
    private String description;
    @CreationTimestamp
    private LocalDateTime contactDate;
    private LocalDateTime nextFollowUp;

}
