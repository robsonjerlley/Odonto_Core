package io.sertaoBit.odontocore.crm.modules.appointment.domain.model;

import io.sertaoBit.odontocore.crm.modules.appointment.domain.enums.AppointmentStatus;
import io.sertaoBit.odontocore.crm.modules.clinic.domain.model.Clinic;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_atendimentos", schema = "crm_db")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    private Clinic clinic;
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    @OneToMany(fetch = FetchType.LAZY)
    private List<Procedure> procedures;
    private Double totalValue;


}
