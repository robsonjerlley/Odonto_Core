package io.sertaoBit.odontocore.crm.modules.identity.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserPasswordUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {

        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PostMapping("/create")
    public ResponseEntity<UserResponseDTO> create(
            @RequestBody @Valid UserCreateRequestDTO requestDTO
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(requestDTO));
    }

    @PatchMapping("/updatePassword/{username}/passwordHash")
    public ResponseEntity<UserResponseDTO> updatePassword(
            @PathVariable String username,
            @RequestBody @Valid UserPasswordUpdateRequestDTO requestDTO
    ) {

        return ResponseEntity.ok(userService.updatePassword(username, requestDTO.newPasswordHash()));
    }


    @GetMapping("/findByUsername/{username}")
    public ResponseEntity<UserResponseDTO> findByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }


    @GetMapping("/findBySector/{sector}")
    public ResponseEntity<List<UserResponseDTO>> findBySector(@PathVariable Sector sector) {
        return ResponseEntity.ok(userService.findBySector(sector));
    }

    @GetMapping("/findBySectorAndRole/{sector}/{role}")
    public ResponseEntity<List<UserResponseDTO>> findBySectorAndRole(
            @PathVariable Sector sector, @PathVariable Role role) {

        return ResponseEntity.ok(userService.findAllBySectorAndRole(sector, role));
    }

    @GetMapping("/existsByUsername/{username}")
    public ResponseEntity<Boolean> existsByUsername(@PathVariable String username) {

        return ResponseEntity.ok(userService.existsByUsername(username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {

        userService.delete(id);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
