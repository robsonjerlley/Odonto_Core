package io.sertaoBit.odontocore.crm.modules.identity.security;

import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class MainUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MainUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(MainUser::form)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }


}
