package io.sertaoBit.odontocore.crm.modules.crm.service.impl;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.ICustomerMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.ICustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements ICustomerService {

    private final ICustomerRepository customerRepository;
    private final ICustomerMapper customerMapper;

    public CustomerServiceImpl(ICustomerRepository customerRepository, ICustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }


    @Override
    public CustomerResponseDTO create(CustomerCreateRequestDTO dto) {
        Customer newCustomer = customerMapper.toEntity(dto);
        Customer customerToSave = customerRepository.save(newCustomer);
        return customerMapper.toResponseDTO(customerToSave);
    }

    @Override
    public CustomerResponseDTO update(String cpf, CustomerUpdateRequestDTO dto) {
        Customer customer = customerRepository.findByCpf(cpf)
                .orElseThrow(() -> new RuntimeException("Customer not found" + cpf));
        customer.setName(dto.name());
        customer.setCpf(dto.cpf());
        customer.setTelephone(dto.telephone());
        customer.setCity(dto.city());
        customer.setAddress(dto.address());
        customer.setTicketStatus(dto.ticketStatus());

        Customer customerToUpdate = customerRepository.save(customer);
        return customerMapper.toResponseDTO(customerToUpdate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> findAll() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findById(UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Customer not found" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findByName(String name) {
        return customerRepository.findByName(name)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Customer not found" + name));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findByCpf(String cpf) {
        return customerRepository.findByCpf(cpf)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Customer not found" + cpf));
    }

    @Override
    public void deleteById(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found" + id);
        }
        customerRepository.deleteById(id);
    }
}
