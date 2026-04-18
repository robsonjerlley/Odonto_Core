package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.modules.identity.domain.Role;
import io.sertaoBit.odontocore.crm.modules.identity.domain.User;

public interface UserService {

    User creat(String username, String password, Role role);
    User findByUsername(String username);
    User update(String username);
    User delete(String username);


}
