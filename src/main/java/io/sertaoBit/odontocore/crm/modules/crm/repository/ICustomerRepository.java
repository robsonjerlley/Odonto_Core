package io.sertaoBit.odontocore.crm.modules.crm.repository;

import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ICustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByNameContainingIgnoreCase(String name);
    Optional<Customer> findByCpf(String cpf);
}
