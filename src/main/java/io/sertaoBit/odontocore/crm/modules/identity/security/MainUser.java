package io.sertaoBit.odontocore.crm.modules.identity.security;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public class MainUser implements UserDetails {

    private UUID id;
    private String username;
    private String passwordHash;
    private Role role;
    private Sector sector;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == null) {
            return Collections.emptyList();
        }

        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public @Nullable String getPassword() {

        return this.passwordHash;
    }


    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isEnabled() {

        return true;
    }

    public static MainUser form(User u) {
        return new MainUser(
                u.getId(),
                u.getUsername(),
                u.getPassword(),
                u.getRole(),
                u.getSector()
        );
    }
}
