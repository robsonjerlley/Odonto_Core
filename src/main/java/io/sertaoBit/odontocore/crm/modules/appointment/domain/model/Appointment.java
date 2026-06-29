package io.sertaoBit.odontocore.crm.modules.appointment.domain.model;

import io.sertaoBit.odontocore.crm.core.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.core.enums.AppointmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name= "appointments", schema = "crm_db")
@NoArgsConstructor @AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of= "id")
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @TenantId
    @Column(nullable = false)
    private UUID clinicId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentType type;
    @Column(nullable = false)
    private UUID ticketId;
    private UUID dealId;
    private UUID procedureId;
    private String procedureName;
    @Column(nullable = false)
    private UUID customerId;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private UUID evaluatorId;
    @Column(nullable = false)
    private UUID assignedTo;
    private LocalDateTime scheduledAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;
    private Integer sessionIndex;
    private Integer plannedSessions;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(columnDefinition = "TEXT")
    private String canceledReason;
    @Column(nullable = false)
    private UUID createdBy;
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
