package io.sertaoBit.odontocore.crm.modules.identity.domain;


import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_users", schema = "crm_db")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;
    @Column(unique = true)
    private String username;
    private String password;

    @OneToOne(cascade = CascadeType.ALL)
    @Enumerated(EnumType.STRING)
    private Role role;

    public User() {}

    public User (String username, String password, Role role) {

    }
    public User (Long id, String username, String password, Role role) {
        this(username, password, role);
    }
}
