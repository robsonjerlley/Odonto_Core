package io.sertaoBit.odontocore.crm.modules.identity.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.request.UserPasswordUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.identity.api.dto.response.UserResponseDTO;
import io.sertaoBit.odontocore.crm.modules.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {

        this.userService = userService;
    }

    @PostMapping()
    public ResponseEntity<UserResponseDTO> create(
            @RequestBody @Valid UserCreateRequestDTO requestDTO
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(requestDTO));
    }

    @PatchMapping("/{username}/newPassword")
    public ResponseEntity<UserResponseDTO> updatePassword(
            @PathVariable String username,
            @RequestBody @Valid UserPasswordUpdateRequestDTO requestDTO
    ) {

        return ResponseEntity.ok(userService.updatePassword(username, requestDTO.newPassword()));
    }


    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> search(
            @RequestParam(required = false) Sector sector,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.search(sector, role, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable  UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponseDTO> findByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {

        userService.delete(id);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
