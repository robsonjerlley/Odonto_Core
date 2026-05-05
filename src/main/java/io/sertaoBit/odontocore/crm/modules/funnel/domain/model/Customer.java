package io.sertaoBit.odontocore.crm.modules.funnel.domain.model;


import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.CustomerSource;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_clientes", schema = "crm_db", indexes = {
        @Index(name = "idx_cpf", columnList = "cpf"),
        @Index(name = "idx_sector_status", columnList = "sector_id, ticket_status")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @NotBlank
    private String name;
    @Column(unique = true)
    @CPF
    private String cpf;
    @NotBlank
    private String telephone;
    @Column(nullable = false)
    @NotBlank
    private String city;
    private String address;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "customer_descriptions"
            , joinColumns = @JoinColumn(name = "customer_id"))
    @Column(length = 500)
    private List<String> descriptions;
    @Enumerated(EnumType.STRING)
    private CustomerSource source;
    @Enumerated(EnumType.STRING)
    private AdsChannel adsChannel;
    private String adCampaign;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Enumerated(EnumType.STRING)
    @JoinColumn(name = "created_by_user_id")
    private UUID createdByUser;


}
