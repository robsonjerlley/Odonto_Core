package io.sertaoBit.odontocore.crm.modules.identity.repository;

import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface IUserRepository extends JpaRepository<User, UUID> {

}
