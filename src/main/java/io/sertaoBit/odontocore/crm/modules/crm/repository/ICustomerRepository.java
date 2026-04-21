package io.sertaoBit.odontocore.crm.modules.crm.repository;

import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface ICustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer>findByName(String name);
    Optional<Customer> findByCPF(String cpf);
}
