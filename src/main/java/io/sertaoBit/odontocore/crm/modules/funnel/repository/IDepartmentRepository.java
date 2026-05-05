package io.sertaoBit.odontocore.crm.modules.funnel.repository;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface IDepartmentRepository extends JpaRepository<Department, UUID> {
}
