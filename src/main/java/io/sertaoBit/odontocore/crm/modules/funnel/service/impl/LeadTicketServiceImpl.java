package io.sertaoBit.odontocore.crm.modules.funnel.service.impl;

import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.ticket.TicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.ticket.TicketUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.TicketResponseDTO;

import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.LeadTicketMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.LeadTicketService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LeadTicketServiceImpl implements LeadTicketService {

    private final LeadTicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final LeadTicketMapper ticketMapper;

    public LeadTicketServiceImpl(
            LeadTicketRepository ticketRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            LeadTicketMapper ticketMapper
    ) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.ticketMapper = ticketMapper;
    }


    @Override
    @Transactional
    public TicketResponseDTO create(TicketCreateRequestDTO dto) {
        Customer customer = customerRepository.findById(dto.customer().getId())
                .orElseThrow(() -> new RuntimeException("Customer with id: " + dto.customer().getId() + " not found"));

        User user = userRepository.findById(dto.assigneTo().getId())
                .orElseThrow(() -> new RuntimeException("User with id: " + dto.assigneTo().getId() + " not found"));

        LeadTicket leadTicket = ticketMapper.toEntity(dto);

        leadTicket.setCustomer(customer);
        leadTicket.setAssigneTo(user);

        return ticketMapper.toResponseDTO(ticketRepository.save(leadTicket));
    }

    @Override
    @Transactional
    public TicketResponseDTO update(UUID id, TicketUpdateRequestDTO dto) {
        LeadTicket leadTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found by id: " + id));

        if (dto.assigneToId() != null) {
            User assignedTo = userRepository.findById(dto.assigneToId())
                    .orElseThrow(() -> new RuntimeException("User with id: " + dto.assigneToId() + " not found"));
            leadTicket.setAssigneTo(assignedTo);
        }

        if (dto.description() != null && !dto.description().isBlank()) {
            leadTicket.setDescription(dto.description());
        }

        if (dto.ticketStatus() != null) {
            leadTicket.setTicketStatus(dto.ticketStatus());
        }

        if (dto.priority() != null) {
            leadTicket.setPriority(dto.priority());
        }

        return ticketMapper.toResponseDTO(ticketRepository.save(leadTicket));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDTO findById(UUID id) {
        return ticketRepository.findById(id)
                .map(ticketMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Ticket not found by id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> findAll() {
        return ticketRepository.findAll().stream()
                .map(ticketMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> findByCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found by id: " + customerId);
        }

        return ticketRepository.findAll().stream()
                .filter(t -> t.getCustomer().getId().equals(customerId))
                .map(ticketMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> findByTicketStatus(TicketStatus ticketStatus) {
        if (ticketStatus == null) {
            throw new RuntimeException("ticketStatus is null");
        }
        return ticketRepository.findAll().stream()
                .filter(t -> t.getTicketStatus().equals(ticketStatus))
                .map(ticketMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> findByAssignedToUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found by id: " + userId);
        }

        return ticketRepository.findAll().stream()
                .filter(t -> t.getAssigneTo() != null && t.getAssigneTo().getId().equals(userId))
                .map(ticketMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TicketResponseDTO updateStatus(UUID id, TicketStatus ticketStatus) {
        LeadTicket leadTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found by id: " + id));

        if (ticketStatus == null) {
            throw new RuntimeException("ticketStatus cannot not be null");
        }

        leadTicket.setTicketStatus(ticketStatus);

        return ticketMapper.toResponseDTO(ticketRepository.save(leadTicket));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (!ticketRepository.existsById(id)) {
            throw new RuntimeException("Ticket not found by id: " + id);
        }

        ticketRepository.deleteById(id);

    }
}
