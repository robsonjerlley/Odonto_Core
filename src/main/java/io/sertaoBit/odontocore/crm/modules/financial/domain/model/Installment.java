package io.sertaoBit.odontocore.crm.modules.financial.domain.model;

import io.sertaoBit.odontocore.crm.core.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "installments", schema = "crm_db", indexes = {
        @Index(name = "idx_inst_clinic_due_status", columnList = "clinic_id, due_date, status"),
        @Index(name = "idx_inst_clinic_customer",   columnList = "clinic_id, customer_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public class Installment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @TenantId
    @Column(name = "clinic_id", nullable = false, updatable = false)
    private UUID clinicId;
    @Column(nullable = false)
    private UUID dealId;
    @Column(nullable = false)
    private UUID customerId;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private Integer sequence;
    @Column(nullable = false)
    private Integer totalInstallments;
    @Column(nullable = false)
    private LocalDate dueDate;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    @Column(precision = 15, scale = 2)
    private BigDecimal paidAmount;
    private LocalDateTime paidAt;
    private UUID paidBy;
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
