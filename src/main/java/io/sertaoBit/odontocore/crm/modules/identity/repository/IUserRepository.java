package io.sertaoBit.odontocore.crm.modules.identity.repository;

import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.shared.enums.Department;
import io.sertaoBit.odontocore.crm.shared.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    List<User> findByDepartment(Department department);

    List<User> findAlByDepartmentAndRole(Department department, Role role);

    Boolean existsByUsername(String username);


}
