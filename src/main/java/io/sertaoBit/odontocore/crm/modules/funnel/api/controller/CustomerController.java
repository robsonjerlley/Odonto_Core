package io.sertaoBit.odontocore.crm.modules.funnel.api.controller;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {

        this.customerService = customerService;
    }

    @PostMapping()
    public ResponseEntity<CustomerResponseDTO> create(
            @RequestBody @Validated CustomerCreateRequestDTO requestDTO
    ) {
        CustomerResponseDTO customerResponseDTO = customerService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerResponseDTO);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> update(
            @PathVariable UUID id, @RequestBody
            @Validated CustomerUpdateRequestDTO requestDTO
    ) {
        return ResponseEntity.ok(customerService.update(id, requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponseDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false)AdsChannel adChannel
            ) {

        return ResponseEntity.ok(customerService.search(name, phone, adChannel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<CustomerResponseDTO> findByCpf(
            @PathVariable @Validated String cpf
    ) {
        return ResponseEntity.ok(customerService.findByCpf(cpf));
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        customerService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
