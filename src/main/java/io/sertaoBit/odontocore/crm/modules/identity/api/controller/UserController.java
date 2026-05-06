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

    @PostMapping("/create")
    public ResponseEntity<UserResponseDTO> create(
            @RequestBody @Valid UserCreateRequestDTO requestDTO, User user
    ) {

        UserResponseDTO userResponseDTO = userService.create(requestDTO, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDTO);
    }

    @PatchMapping("/updatePassword/{username}/password")
    public ResponseEntity<UserResponseDTO> updatePassword(
            @PathVariable String username,
            @RequestBody @Valid UserPasswordUpdateRequestDTO requestDTO
    ) {

        return ResponseEntity.ok(userService.updatePassword(username, requestDTO.newPassword()));
    }


    @GetMapping("/{username}")
    public ResponseEntity<UserResponseDTO> findByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }


    @GetMapping("/{sector}")
    public ResponseEntity<List<UserResponseDTO>> findBySector(@PathVariable Sector sector) {
        return ResponseEntity.ok(userService.findBySector(sector));
    }

    @GetMapping("/finBySectorAndRole/{sector}/{role}")
    public ResponseEntity<List<UserResponseDTO>> findBySectorAndRole(
            @PathVariable Sector sector, @PathVariable Role role) {

        return ResponseEntity.ok(userService.findAllBySectorAndRole(sector, role));
    }

    @GetMapping("/existsByUsername/{username}")
    public ResponseEntity<UserResponseDTO> existsByUsername(@PathVariable String username) {

        return ResponseEntity.ok(userService.findByUsername(username));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {

        userService.delete(id);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
