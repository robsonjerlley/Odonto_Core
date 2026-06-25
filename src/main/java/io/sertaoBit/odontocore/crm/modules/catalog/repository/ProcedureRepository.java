package io.sertaoBit.odontocore.crm.modules.catalog.repository;

import io.sertaoBit.odontocore.crm.modules.catalog.domain.model.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, UUID>, JpaSpecificationExecutor<Procedure> {


}
