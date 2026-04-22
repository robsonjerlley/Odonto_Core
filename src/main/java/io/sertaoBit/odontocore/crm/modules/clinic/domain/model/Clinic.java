package io.sertaoBit.odontocore.crm.modules.clinic.domain.model;

import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.br.CNPJ;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_clinicas", schema = "clinic_db")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String name;
    @CNPJ
    @Column(unique = true, nullable = false)
    private String cnpj;
    private String telephone;
    private String address;
    private String city;

    @OneToMany(mappedBy = "clinic")
    private List<User> employees = new ArrayList<>();


}
