package io.sertaoBit.odontocore.crm.modules.crm.repository;

import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Costumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface ICostumerRepository extends JpaRepository<Costumer, UUID> {

    Optional<Costumer>findByName(String name);
    Optional<Costumer> findByTelephone(String telephone);
    Optional<Costumer> findByCPF(String cpf);
}
