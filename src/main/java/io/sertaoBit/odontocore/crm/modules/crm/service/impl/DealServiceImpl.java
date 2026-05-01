package io.sertaoBit.odontocore.crm.modules.crm.service.impl;

import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.request.deal.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.crm.api.dto.response.DealResponseDTO;
import io.sertaoBit.odontocore.crm.modules.crm.domain.enums.DealStatus;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.crm.domain.model.Deal;
import io.sertaoBit.odontocore.crm.modules.crm.mapper.IDealMapper;
import io.sertaoBit.odontocore.crm.modules.crm.repository.ICustomerRepository;
import io.sertaoBit.odontocore.crm.modules.crm.repository.IDealRepository;
import io.sertaoBit.odontocore.crm.modules.crm.service.IDealService;
import io.sertaoBit.odontocore.crm.modules.identity.repository.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DealServiceImpl implements IDealService {

    private final IDealRepository dealRepository;
    private final IUserRepository userRepository;
    private final ICustomerRepository customerRepository;
    private final IDealMapper dealMapper;

    public DealServiceImpl(
            IDealRepository dealRepository,
            IUserRepository userRepository,
            ICustomerRepository customerRepository,
            IDealMapper dealMapper
    ) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.dealMapper = dealMapper;
    }

    @Override
    @Transactional
    public DealResponseDTO create(DealCreateRequestDTO dto) {
        Customer customer = customerRepository.findById(dto.customer().getId())
                .orElseThrow(() -> new RuntimeException("Customer Not Found by id " + dto.customer().getId()));

        Deal deal = dealMapper.toEntity(dto);
        deal.setCustomer(customer);

        // Deal começa em NEGOTIATING sem closedBy definido
        deal.setDealStatus(DealStatus.NEGOTIATING);
        deal.setClosedBy(null);


        Deal saved = dealRepository.save(deal);
        return dealMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public DealResponseDTO update(UUID id, DealUpdateRequestDTO dto) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deal not found by id: " + id));

        if (dto.description() != null && !dto.description().isBlank()) {
            deal.setDescription(dto.description());
        }

        if (dto.negotiationValue() != null) {
            deal.setNegotiationValue(dto.negotiationValue());
        }

        if (dto.procedures() != null && !dto.procedures().isEmpty()) {
            deal.setProcedures(Set.of(dto.procedures().toString()));
        }

        Deal updated = dealRepository.save(deal);
        return dealMapper.toResponseDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public DealResponseDTO findById(UUID id) {
        return dealRepository.findById(id)
                .map(dealMapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Deal Not Found by id " + id));

    }

    @Override
    @Transactional(readOnly = true)
    public List<DealResponseDTO> findAll() {
        return dealRepository.findAll().stream()
                .map(dealMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealResponseDTO> findByCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new RuntimeException("Customer Not Found by id " + customerId);
        }
        return dealRepository.findAll().stream()
                .filter(deal -> deal.getCustomer().getId().equals(customerId))
                .map(dealMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealResponseDTO> findByStatus(DealStatus status) {
        if (status == null) {
            throw new RuntimeException("Deal Status Not Found");
        }

        return dealRepository.findAll().stream()
                .filter(deal -> deal.getDealStatus().equals(status))
                .map(dealMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealResponseDTO> findClosedByUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User Not Found by id " + userId);
        }

        return dealRepository.findAll().stream()
                .filter(deal -> deal.getClosedBy().getId().equals(userId))
                .map(dealMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealResponseDTO> findByDateRange(LocalDate start, LocalDate end) {
        return dealRepository.findAll().stream()
                .filter(deal -> deal.getClosedDate() != null
                        && deal.getClosedDate().isAfter(ChronoLocalDateTime.from(start))
                        && deal.getClosedDate().isBefore(ChronoLocalDateTime.from(end)))
                .map(dealMapper::toResponseDTO)
                .collect(Collectors.toList());

    }

    @Override
    @Transactional
    public DealResponseDTO updateStatus(UUID id, DealStatus status) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deal not found by id: " + id));

        if (status == null) {
            throw new RuntimeException("Deal status cannot be null");
        }

        DealStatus currentStatus = deal.getDealStatus();

        if (currentStatus == DealStatus.WON || currentStatus == DealStatus.LOST) {
            throw new RuntimeException("Cannot update Deal in terminal status: " + currentStatus);
        }


        deal.setDealStatus(status);

        Deal updated = dealRepository.save(deal);
        return dealMapper.toResponseDTO(updated);
    }


    @Override
    @Transactional
    public void delete(UUID id) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deal not found by id: " + id));

        if (deal.getDealStatus() == DealStatus.WON || deal.getDealStatus() == DealStatus.LOST) {
            throw new RuntimeException("Cannot delete Deal in terminal status: " + deal.getDealStatus());
        }

        dealRepository.deleteById(id);
    }
}

