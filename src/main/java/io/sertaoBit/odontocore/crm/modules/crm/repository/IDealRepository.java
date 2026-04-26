package io.sertaoBit.odontocore.crm.modules.crm.repository;

import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IDealRepository extends JpaRepository<Deal, UUID> {
}
