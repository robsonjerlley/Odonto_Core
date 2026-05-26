package io.sertaoBit.odontocore.crm.modules.funnel.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.exception.ResourceAlreadyExistsException;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.CustomerMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final LeadTicketRepository leadTicketRepository;
    private final CustomerMapper customerMapper;
    private final SecurityUtils securityUtils;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            LeadTicketRepository leadTicketRepository,
            CustomerMapper customerMapper,
            SecurityUtils securityUtils
    ) {
        this.customerRepository = customerRepository;
        this.leadTicketRepository = leadTicketRepository;
        this.customerMapper = customerMapper;
        this.securityUtils = securityUtils;
    }


    @Override
    @Transactional
    public CustomerResponseDTO create(CustomerCreateRequestDTO dto) {
        var currentUser = securityUtils.getCurrentUser();

        Customer customer = Customer.builder()
                .name(dto.name())
                .cpf(dto.cpf())
                .phone(dto.phone())
                .email(dto.email())
                .source(dto.source())
                .adChannel(dto.adChannel())
                .adCampaign(dto.adCampaign())
                .createdBy(currentUser.getId())
                .referredBy(dto.referredBy())
                .build();

        Customer saved = customerRepository.save(customer);

        LeadTicket ticket = LeadTicket.builder()
                .customerId(saved.getId())
                .status(TicketStatus.NEW)
                .currentSector(currentUser.getSector())
                .createdBy(currentUser.getId())
                .build();
        leadTicketRepository.save(ticket);

        return customerMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public CustomerResponseDTO update(UUID id, CustomerUpdateRequestDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found " + id));

        if (!customer.getCpf().equals(dto.cpf()) && customerRepository.findByCpf(dto.cpf()).isPresent()) {
            throw new ResourceAlreadyExistsException("CPF " + dto.cpf() + " já existe na base de dados");
        }
        customer.setName(dto.name());
        customer.setCpf(dto.cpf());
        customer.setEmail(dto.email());
        customer.setPhone(dto.phone());
        return customerMapper.toResponseDTO(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> findAll() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findById(UUID id) {
        return customerRepository.findById(id)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> findByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name).stream()
                .map(customerMapper::toResponseDTO)
               .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findByCpf(String cpf) {
        return customerRepository.findByCpf(cpf)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found by CPF " + cpf));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> findByAdChannel(AdsChannel channel) {
        return customerRepository.findByAdChannel(channel).stream()
                .map(customerMapper::toResponseDTO).toList();

    }


    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found " + id);
        }
        customerRepository.deleteById(id);
    }
}
