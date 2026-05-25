package io.sertaoBit.odontocore.crm.modules.funnel.domain.model;


import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.CustomerSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
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
    @Column(nullable = false)
    private String name;
    @Column(unique = true)
    private String cpf;
    @Column(nullable = false)
    private String phone;
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerSource source;
    @Enumerated(EnumType.STRING)
    private AdsChannel adChannel;
    private String adCampaign;
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private UUID createdBy;
    private UUID referredBy;


}
