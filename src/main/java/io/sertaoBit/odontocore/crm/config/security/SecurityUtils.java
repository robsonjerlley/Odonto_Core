package io.sertaoBit.odontocore.crm.config.security;

import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import io.sertaoBit.odontocore.crm.modules.identity.service.IUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    private final IUserRepository userRepository;

    public SecurityUtils(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


        if(authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("Authentication object is null");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assert userDetails != null;
        String username = userDetails.getUsername();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("Username not found"));

    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}


