package io.sertaoBit.odontocore.crm.modules.funnel.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.leadTicket.LeadTicketCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.LeadTicketResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.LeadTicket;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.LeadTicketMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.LeadTicketServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.NEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("LeadTicketService - Testes Unitários do Serviço")
public class LeadTicketServiceTest {

    private LeadTicketService leadTicketService;
    @Mock
    private LeadTicketRepository ticketRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ContactLogRepository contactLogRepository;
    @Mock
    private LeadTicketMapper ticketMapper;

    @BeforeEach
    void setUp(){

        leadTicketService = new LeadTicketServiceImpl(
                ticketRepository,
                customerRepository,
                userRepository,
                ticketMapper,
                securityUtils,
                contactLogRepository);


    }

    @Test
    @DisplayName("Deve criar um ticket com sucesso")
    void create() {

        UUID userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).build();
        when(customerRepository.existsById(customerId)).thenReturn(true);

        LeadTicketCreateRequestDTO dto = new LeadTicketCreateRequestDTO(
                customerId, Sector.LEADS, userId,
                LocalDateTime.of(2026, 6 , 16, 16, 25)
        );

        LeadTicket leadTicket = LeadTicket.builder()
                .id(UUID.randomUUID())
                .customerId(dto.customerId())
                .status(NEW)
                .currentSector(dto.currentSector())
                .assignedTo(dto.assignedTo())
                .scheduledAt(dto.scheduledAt())
                .createdBy(userId)
                .build();

        when(ticketRepository.save(any(LeadTicket.class))).thenReturn(leadTicket);

        LeadTicketResponseDTO expectedDTO = new LeadTicketResponseDTO(
                leadTicket.getId(),
                leadTicket.getCustomerId(),
                leadTicket.getStatus(),
                leadTicket.getCurrentSector(),
                leadTicket.getAssignedTo(),
                leadTicket.getScheduledAt(),
                null,
                null,
                leadTicket.getCreatedBy(),
                null,
                LocalDateTime.now(),
                null,
                null
        );

        when(ticketMapper.toResponseDTO(any(LeadTicket.class))).thenReturn(expectedDTO);
        LeadTicketResponseDTO result = leadTicketService.create(dto);

        assertNotNull(expectedDTO);
        assertEquals(dto.customerId(), result.customerId());
        assertEquals(dto.currentSector(), result.currentSector());
        assertEquals(dto.scheduledAt(), result.scheduledAt());
        assertEquals(dto.assignedTo(), result.assignedTo());

        verify(ticketRepository, times(1) ).save(any(LeadTicket.class));
        verify(securityUtils, times(1)).getCurrentUserId();
        verify(ticketMapper, times(1)).toResponseDTO(any(LeadTicket.class));

    }

}
