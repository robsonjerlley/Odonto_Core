package io.sertaoBit.odontocore.crm.modules.funnel.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
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
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerSpecifications;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.CustomerService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.*;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.CUSTOMER;
import static io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerSpecifications.*;
import static io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerSpecifications.byScope;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final LeadTicketRepository leadTicketRepository;
    private final ContactLogRepository contactLogRepository;
    private final CustomerMapper customerMapper;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;


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
                .adsChannel(dto.adsChannel())
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
                customer.getCreatedBy()
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
        customer.setPhone2(dto.phone2());
        return customerMapper.toResponseDTO(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDTO> search(String phone, String name, AdsChannel asdChannel, Pageable pageable) {
        User user = securityUtils.getCurrentUser();

        PermissionScope scope = permissionService.getScope(
                user,
                CUSTOMER,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access denied"));

        Specification<Customer> spec = Specification
                .where(byScope(scope, user))
                .and(notAnonymized())
                .and(hasPhoneNumber(phone))
                .and(hasName(name))
                .and(hasAdsChannel(asdChannel));

        return customerRepository.findAll(spec, pageable)
                .map(customerMapper::toResponseDTO);
    }


    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO findById(UUID id) {
        User user = securityUtils.getCurrentUser();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found " + id));
        permissionService.checkOrThrow(
                user,
                CUSTOMER,
                READ,
                user.getSector(),
                customer.getCreatedBy()
        );

        return customerMapper.toResponseDTO(customer);

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
                user.getId()
        );

        return customerRepository.findByCpf(cpf)
                .map(customerMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found by CPF " + cpf));
    }


    @Override
    @Transactional
    public void anonymize(UUID id) {
        User user = securityUtils.getCurrentUser();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found " + id));

        permissionService.checkOrThrow(
                user,
                CUSTOMER,
                DELETE,
                user.getSector(),
                customer.getCreatedBy()
        );

        customer.setName("CLIENTE ANONIMIZADO");
        customer.setCpf(null);
        customer.setPhone("NULL");
        customer.setPhone2(null);
        customer.setEmail(null);
        customer.setInitialNote(null);
        customer.setAddress(null);
        customer.setReferredBy(null);
        customer.setAnonymized(true);

        customerRepository.save(customer);
    }

}
