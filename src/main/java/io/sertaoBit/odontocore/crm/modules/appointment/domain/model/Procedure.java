package io.sertaoBit.odontocore.crm.modules.appointment.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_procedimentos", schema = "crm_db")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @NotBlank
    private String name;
    @NotBlank
    @Column(length = 350)
    private String description;
    @NonNull
    private Double basePrice;
    @NotBlank
    private String category;

}
