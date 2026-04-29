package io.sertaoBit.odontocore.crm.modules.crm.service.impl;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket.TicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.ticket.TicketUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.TicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Ticket;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.ITicketMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ITicketRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.ITicketService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements ITicketService {

    private final ITicketRepository ticketRepository;
    private final ICustomerRepository customerRepository;
    private final IUserRepository userRepository;
    private final ITicketMapper ticketMapper;

    public TicketServiceImpl(
            ITicketRepository ticketRepository,
            ICustomerRepository customerRepository,
            IUserRepository userRepository,
            ITicketMapper ticketMapper
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

        Ticket ticket = ticketMapper.toEntity(dto);

        ticket.setCustomer(customer);
        ticket.setAssigneTo(user);

        return ticketMapper.toResponseDTO(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponseDTO update(UUID id, TicketUpdateRequestDTO dto) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found by id: " + id));

        if (dto.assigneToId() != null) {
            User assignedTo = userRepository.findById(dto.assigneToId())
                    .orElseThrow(() -> new RuntimeException("User with id: " + dto.assigneToId() + " not found"));
            ticket.setAssigneTo(assignedTo);
        }

        if (dto.description() != null && !dto.description().isBlank()) {
            ticket.setDescription(dto.description());
        }

        if (dto.ticketStatus() != null) {
            ticket.setTicketStatus(dto.ticketStatus());
        }

        if (dto.priority() != null) {
            ticket.setPriority(dto.priority());
        }

        return ticketMapper.toResponseDTO(ticketRepository.save(ticket));
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
    public List<TicketResponseDTO> findByAssignedTo(UUID userId) {
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
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found by id: " + id));

        if (ticketStatus == null) {
            throw new RuntimeException("ticketStatus cannot not be null");
        }

        ticket.setTicketStatus(ticketStatus);

        return ticketMapper.toResponseDTO(ticketRepository.save(ticket));
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
