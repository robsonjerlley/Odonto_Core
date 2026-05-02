package io.sertaoBit.odontocore.crm.modules.crm.service.impl;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.contactLog.ContactLogUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.ContactOutcome;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Ticket;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.IContactLogMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.IContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ITicketRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.IContactLogService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContactLogServiceImpl implements IContactLogService {

    private final IContactLogRepository contactLogRepository;
    private final IContactLogMapper contactLogMapper;
    private final ICustomerRepository customerRepository;
    private final ITicketRepository ticketRepository;
    private final IUserRepository userRepository;

    public ContactLogServiceImpl(
            IContactLogRepository contactLogRepository,
            IContactLogMapper contactLogMapper,
            ICustomerRepository customerRepository,
            ITicketRepository ticketRepository,
            IUserRepository userRepository
    ) {
        this.contactLogRepository = contactLogRepository;
        this.contactLogMapper = contactLogMapper;
        this.customerRepository = customerRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ContactLogResponseDTO create(ContactLogCreateRequestDTO dto) {
        Objects.requireNonNull(dto.customer(), "Customer must not be null");
        Objects.requireNonNull(dto.customer().getId(), "Customer id must not be null");

        Customer customer = customerRepository.findById(dto.customer().getId())
                .orElseThrow(() -> new RuntimeException("Customer not found by id: " + dto.customer().getId()));

        Ticket ticket = null;
        if (dto.ticket() != null && dto.ticket().getId() != null) {
            ticket = ticketRepository.findById(dto.ticket().getId())
                    .orElseThrow(() -> new RuntimeException("Ticket not found by id: " + dto.ticket().getId()));

            if (!ticket.getCustomer().getId().equals(customer.getId())) {
                throw new RuntimeException("Ticket does not belong to this Customer");
            }
        }

        User contactBy = userRepository.findById(dto.contactBy().getId())
                .orElseThrow(() -> new RuntimeException("User not found by id: " + dto.contactBy().getId()));

        ContactLog contactLog = contactLogMapper.toEntity(dto);

        contactLog.setCustomer(customer);
        contactLog.setTicket(ticket);
        contactLog.setContactBy(contactBy);

        return contactLogMapper.toResponseDTO(contactLogRepository.save(contactLog));
    }

    @Override
    @Transactional
    public ContactLogResponseDTO update(UUID id, ContactLogUpdateRequestDTO dto) {
        ContactLog contactLog = contactLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ContactLog not found by id: " + id));

        if (dto.description() != null && !dto.description().isBlank()) {
            contactLog.setDescription(dto.description());
        }

        if (dto.contactOutcome() != null) {
            contactLog.setContactOutcome(dto.contactOutcome());
        }

        if (dto.nextFollowUp() != null) {
            contactLog.setNextFollowUp(dto.nextFollowUp());
        }

        if (dto.investmentAmount() != null) {
            contactLog.setInvestmentAmount(dto.investmentAmount());
        }

        if (dto.conversionValue() != null) {
            contactLog.setConversionValue(dto.conversionValue());
        }

        return contactLogMapper.toResponseDTO(contactLogRepository.save(contactLog));
    }

    @Override
    @Transactional(readOnly = true)
    public ContactLogResponseDTO findById(UUID id) {
        return contactLogRepository.findById(id)
                .map(contactLogMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("ContactLog not found by id: " + id));
    }


    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findAll() {
        return contactLogRepository.findAll().stream()
                .map(contactLogMapper::toResponseDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findByCustomer(UUID id) {
        Objects.requireNonNull(id, "Customer id must not be null");

        return contactLogRepository.findByCustomerId(id).stream()
                .filter(cl -> cl.getCustomer().getId().equals(id))
                .map(contactLogMapper::toResponseDTO)
                .collect(Collectors.toList());

    }


    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findByContactByUser(UUID id) {
        Objects.requireNonNull(id, "Contact id must not be null");

        return contactLogRepository.findAll().stream()
                .filter(cl -> cl.getContactBy().getId().equals(id))
                .map(contactLogMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findByChannel(ContactChannel channel) {
        Objects.requireNonNull(channel, "Channel must not be null");

        return contactLogRepository.findByContactChannel(channel).stream()
                .filter(cl -> cl.getContactChannel().equals(channel))
                .map(contactLogMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findOutcome(ContactOutcome contactOutcome) {
        return contactLogRepository.findByContactOutcomes(contactOutcome).stream()
                .filter(cl -> cl.getContactOutcome() == contactOutcome)
                .map(contactLogMapper::toResponseDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findByDateRange(LocalDate start, LocalDate end) {
        return contactLogRepository.findAll().stream()
                .filter(cl -> cl.getContactDate() != null
                        && cl.getContactDate().isAfter(ChronoLocalDateTime.from(start))
                        && cl.getContactDate().isBefore(ChronoLocalDateTime.from(end)))
                .map(contactLogMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retorna TODOS os contactos com follow-up pendente.
     * <p>
     * Follow-up é pendente quando:
     * - nextFollowUp != null
     * - nextFollowUp <= hoje
     */
    @Override
    @Transactional(readOnly = true)
    public List<ContactLogResponseDTO> findWithPendingFollowUp() {
        return contactLogRepository.findAll().stream()
                .filter(cl -> cl.getNextFollowUp() != null
                        && !cl.getNextFollowUp().isAfter(java.time.LocalDate.now()))
                .map(contactLogMapper::toResponseDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void delete(UUID id) {
        if (!contactLogRepository.existsById(id)) {
            throw new RuntimeException("ContactLog not found by id: " + id);
        }
        contactLogRepository.deleteById(id);
    }
}
