package io.sertaoBit.odontocore.crm.modules.crm.service.impl;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.ICustomerMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.ICustomerService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements ICustomerService {

    private final ICustomerRepository customerRepository;
    private final ICustomerMapper customerMapper;
    private final IUserRepository userRepository;

    public CustomerServiceImpl(ICustomerRepository customerRepository, ICustomerMapper customerMapper, IUserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.userRepository = userRepository;
    }


    @Override
    @Transactional
    public CustomerResponseDTO create(CustomerCreateRequestDTO dto) {
        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new RuntimeException("User not found by id: " + dto.userId()));

        Customer newCustomer = customerMapper.toEntity(dto);
        newCustomer.setUser(user);

        if(dto.description() != null){
            newCustomer.getDescription().add(dto.description());
        }

        Customer customerToSave = customerRepository.save(newCustomer);
        return customerMapper.toResponseDTO(customerToSave);
    }

    @Override
    @Transactional
    public CustomerResponseDTO update(String cpf, CustomerUpdateRequestDTO dto) {
        Customer customer = customerRepository.findByCpf(cpf)
                .orElseThrow(() -> new RuntimeException("Customer not found " + cpf));

        if(!customer.getCpf().equals(dto.cpf()) && customerRepository.findByCpf(dto.cpf())
                .isPresent()) {
            throw new RuntimeException("O novo CPF informado  " + dto.cpf() + " já pertence a outro cliente");
        }
        if (dto.description() != null && !dto.description().isBlank()) {
            customer.getDescription().add(dto.description());
        }
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
                .orElseThrow(() -> new RuntimeException("Customer not found " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> findByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name).stream()
                .map(customerMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findByCpf(String cpf) {
        return customerRepository.findByCpf(cpf)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Customer not found " + cpf));
    }

    @Override
    public void deleteById(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found " + id);
        }
        customerRepository.deleteById(id);
    }
}
