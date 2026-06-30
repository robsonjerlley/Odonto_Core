package io.sertaoBit.odontocore.crm.modules.financial.repository;

import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, UUID> {
}
