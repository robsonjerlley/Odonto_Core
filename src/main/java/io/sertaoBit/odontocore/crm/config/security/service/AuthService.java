package io.sertaoBit.odontocore.crm.config.security.service;

import io.sertaoBit.odontocore.crm.config.security.JwtUtil;
import io.sertaoBit.odontocore.crm.config.security.api.dto.response.AuthResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.security.MainUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthService {


    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }


    public AuthResponseDTO login(String username, String password) {
        var authToken = new UsernamePasswordAuthenticationToken(username, password);
        var authentication = authenticationManager.authenticate(authToken);
        var mainUser = (MainUser) authentication.getPrincipal();
        var token = jwtUtil.generateToken(Objects.requireNonNull(mainUser));

        return new AuthResponseDTO(token, "Bearer");
    }


}
