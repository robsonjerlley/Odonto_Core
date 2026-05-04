package io.sertaoBit.odontocore.crm.modules.crm.repository;

import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.ContactLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface IContactLogRepository extends JpaRepository<ContactLog, UUID> {

  List<ContactLog> findByContactDate(LocalDate date);

  List<ContactLog> findByCustomerId(UUID customerId);

  List<ContactLog>findByContactChannel(ContactChannel channel);

  List<ContactLog> findByContactOutcomes(ContactOutcome contactOutcome);
  ;

}
