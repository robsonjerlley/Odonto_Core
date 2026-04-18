package io.sertaoBit.odontocore.crm.modules.identity.service.impl;

import io.sertaoBit.odontocore.crm.modules.identity.domain.Role;
import io.sertaoBit.odontocore.crm.modules.identity.domain.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.UserService;

public class UserServiceImpl implements UserService {

    private final IUserRepository userRepository;

    public UserServiceImpl(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User creat(String username, String password, Role role) {
        return null;
    }

    @Override
    public User findByUsername(String username) {
        return null;
    }

    @Override
    public User update(String username) {
        return null;
    }

    @Override
    public User delete(String username) {
        return null;
    }
}
