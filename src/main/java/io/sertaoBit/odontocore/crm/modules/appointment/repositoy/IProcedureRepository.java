package io.sertaoBit.odontocore.crm.modules.appointment.repositoy;

import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IProcedureRepository extends JpaRepository<Procedure, UUID> {

    List<Procedure> findByName(String name);
}
