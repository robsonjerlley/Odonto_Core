package io.sertaoBit.odontocore.crm.modules.crm.api.controller;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.service.ICustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final ICustomerService customerService;

    public CustomerController(ICustomerService customerService) {

        this.customerService = customerService;
    }

    @PostMapping("/create")
    public ResponseEntity<CustomerResponseDTO> create(
            @RequestBody @Valid CustomerCreateRequestDTO requestDTO) {
        CustomerResponseDTO customerResponseDTO = customerService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerResponseDTO);
    }

    @PatchMapping("/cpf/{cpf}")
    public ResponseEntity<CustomerResponseDTO> update(
            @PathVariable String cpf, @RequestBody
            @Valid CustomerUpdateRequestDTO requestDTO) {
        return ResponseEntity.ok(customerService.update(cpf, requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponseDTO>> findAll() {

        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @GetMapping("cpf/{cpf}")
    public ResponseEntity<CustomerResponseDTO> findByCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(customerService.findByCpf(cpf));
    }


    @GetMapping("/name/{name}")
    public ResponseEntity<List<CustomerResponseDTO>> findByName(@PathVariable String name) {
       List<CustomerResponseDTO> customersDTO= customerService.findByName(name);
       return ResponseEntity.ok(customersDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
