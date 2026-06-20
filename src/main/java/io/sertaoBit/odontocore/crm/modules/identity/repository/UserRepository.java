package io.sertaoBit.odontocore.crm.modules.identity.repository;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Page<User> findBySector(Sector sector, Pageable pageable);

    Page<User> findAllBySectorAndRole(Sector sector, Role role, Pageable pageable);

    List<User> findByActiveTrue();

    Optional<User>findByIdAndClinicId(UUID id, UUID clinicId);

}
