package io.sertaoBit.odontocore.crm.config.security.api.controller;

import io.sertaoBit.odontocore.crm.config.security.api.dto.request.AuthRequestDTO;
import io.sertaoBit.odontocore.crm.config.security.api.dto.request.RefreshTokenRequestDTO;
import io.sertaoBit.odontocore.crm.config.security.api.dto.response.AuthResponseDTO;
import io.sertaoBit.odontocore.crm.config.security.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {

        this.authService = authService;
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @RequestBody @Validated AuthRequestDTO dto
    ) {
        return ResponseEntity.ok(authService.login(dto.username(), dto.password()));
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(
            @RequestBody @Validated RefreshTokenRequestDTO dto
    ) {
        return ResponseEntity.ok(authService.refreshToken(dto));
    }
}
