package io.sertaoBit.odontocore.crm.modules.appointment.repositoy;

import io.sertaoBit.odontocore.crm.modules.appointment.api.dto.response.ProcedureResponseDTO;
import io.sertaoBit.odontocore.crm.modules.appointment.domain.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IProcedureRepository extends JpaRepository<Appointment, UUID> {

    List<ProcedureResponseDTO> finByName(String name);
}
