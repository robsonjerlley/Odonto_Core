package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @NullMarked
    Page<Customer> findAll(Pageable pageable);

    Optional<Customer> findByCpf(String cpf);

    Page<Customer> findByPhone(String phone, Pageable pageable);

    List<Customer> findByAdChannel(AdsChannel adChannel);

    Page<Customer> findByAdChannel(AdsChannel adChannel, Pageable pageable);

}
