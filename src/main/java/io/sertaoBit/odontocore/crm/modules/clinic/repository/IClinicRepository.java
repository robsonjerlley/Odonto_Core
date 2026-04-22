package io.sertaoBit.odontocore.crm.modules.clinic.repository;

import io.sertaoBit.odontocore.crm.modules.clinic.domain.model.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IClinicRepository extends JpaRepository<Clinic, UUID> {

    Optional<Clinic> findByCnpj(String cnpj);


}
