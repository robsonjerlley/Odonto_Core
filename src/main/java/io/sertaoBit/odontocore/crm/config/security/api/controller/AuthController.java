package io.sertaoBit.odontocore.crm.config.security.api.controller;

import io.sertaoBit.odontocore.crm.config.security.JwtUtil;
import io.sertaoBit.odontocore.crm.config.security.api.dto.request.AuthRequestDTO;
import io.sertaoBit.odontocore.crm.config.security.api.dto.response.AuthResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authentication")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;


    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> authenticate(
            @RequestBody @Validated AuthRequestDTO dto
    ) {
        var authToken = new UsernamePasswordAuthenticationToken(dto.username(), dto.password());
        var authentication = authenticationManager.authenticate(authToken);
        var userDetails = (UserDetails) authentication.getPrincipal();
        var token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponseDTO(token, "Bearer"));

    }
}
