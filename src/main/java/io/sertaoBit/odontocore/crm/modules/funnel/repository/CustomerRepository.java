package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.CustomerSource;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByNameContainingIgnoreCase(String name);

    Optional<Customer> findByCpf(String cpf);

    List<Customer> findByPhone(String phone);

    List<Customer> findBySource(CustomerSource source);

    List<Customer> findByAdChannel(AdsChannel adChannel);


}
