package io.sertaoBit.odontocore.crm.modules.funnel.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.*;
import io.sertaoBit.odontocore.crm.exception.ResourceAlreadyExistsException;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.customer.CustomerUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.CustomerResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.CustomerMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.CustomerService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.*;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.CUSTOMER;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final LeadTicketRepository leadTicketRepository;
    private final ContactLogRepository contactLogRepository;
    private final CustomerMapper customerMapper;
    private final SecurityUtils securityUtils;
    private final PermissionService  permissionService;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            LeadTicketRepository leadTicketRepository,
            ContactLogRepository contactLogRepository,
            CustomerMapper customerMapper,
            SecurityUtils securityUtils,
            PermissionService permissionService
    ) {
        this.customerRepository = customerRepository;
        this.leadTicketRepository = leadTicketRepository;
        this.contactLogRepository = contactLogRepository;
        this.customerMapper = customerMapper;
        this.securityUtils = securityUtils;
        this.permissionService = permissionService;
    }


    @Override
    @Transactional
    public CustomerResponseDTO create(CustomerCreateRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                CUSTOMER,
                CREATE,
                user.getSector(),
                user.getId()
        );


        Customer customer = Customer.builder()
                .name(dto.name())
                .cpf(dto.cpf())
                .phone(dto.phone())
                .phone2(dto.phone2())
                .email(dto.email())
                .initialNote(dto.initialNote())
                .source(dto.source())
                .adChannel(dto.adChannel())
                .adCampaign(dto.adCampaign())
                .createdBy(user.getId())
                .referredBy(dto.referredBy())
                .build();

        Customer saved = customerRepository.save(customer);

        LeadTicket ticket = LeadTicket.builder()
                .customerId(saved.getId())
                .status(TicketStatus.NEW)
                .currentSector(user.getSector())
                .createdBy(user.getId())
                .build();
        leadTicketRepository.save(ticket);

        if (dto.initialNote() != null && !dto.initialNote().isBlank()) {
            ContactLog contactLog = ContactLog.builder()
                    .ticketId(ticket.getId())
                    .userId(user.getId())
                    .channel(dto.channel() != null ? dto.channel() : ContactChannel.OTHER)
                    .note(dto.initialNote())
                    .occurredAt(LocalDateTime.now())
                    .build();
            contactLogRepository.save(contactLog);
        }

        return customerMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public CustomerResponseDTO update(UUID id, CustomerUpdateRequestDTO dto) {
        User user = securityUtils.getCurrentUser();

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found " + id));

        permissionService.checkOrThrow(
                user,
                CUSTOMER,
                UPDATE,
                user.getSector(),
                user.getCreatedBy()
        );

        if (!Objects.equals(customer.getCpf(), dto.cpf())
                && dto.cpf() != null
                && customerRepository.findByCpf(dto.cpf()).isPresent()
        ) {
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
    public Page<CustomerResponseDTO> search(String phone, String name, AdsChannel adChannel, Pageable pageable) {
        if (phone != null) return findByPhone(phone, pageable);
        if (name != null) return findByName(name, pageable);
        if (adChannel != null) return findByAdChannel(adChannel, pageable);
        return findAll(pageable);
    }


    private Page<CustomerResponseDTO> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(customerMapper::toResponseDTO);

    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findById(UUID id) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                CUSTOMER,
                READ,
                user.getSector(),
                user.getCreatedBy()
        );

        return customerRepository.findById(id)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found " + id));
    }

    private Page<CustomerResponseDTO> findByName(String name, Pageable pageable) {
        return customerRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(customerMapper::toResponseDTO);

    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findByCpf(String cpf) {
        User user = securityUtils.getCurrentUser();
        permissionService.checkOrThrow(
                user,
                CUSTOMER,
                READ,
                user.getSector(),
                user.getCreatedBy()
        );

        return customerRepository.findByCpf(cpf)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found by CPF " + cpf));
    }


    private Page<CustomerResponseDTO> findByPhone(String phone, Pageable pageable) {
        return customerRepository.findByPhone(phone, pageable)
                .map(customerMapper::toResponseDTO);

    }

    private Page<CustomerResponseDTO> findByAdChannel(AdsChannel channel, Pageable pageable) {
        return customerRepository.findByAdChannel(channel, pageable)
                .map(customerMapper::toResponseDTO);

    }


    @Override
    @Transactional
    public void deleteById(UUID id) {
        User user = securityUtils.getCurrentUser();
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found " + id);
        }

        permissionService.checkOrThrow(
                user,
                CUSTOMER,
                DELETE,
                user.getSector(),
                user.getCreatedBy()
        );

        customerRepository.deleteById(id);
    }
}
