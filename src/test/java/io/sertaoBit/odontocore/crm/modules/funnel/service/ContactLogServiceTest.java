package io.sertaoBit.odontocore.crm.modules.funnel.service;


import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.ContactChannel;
import io.sertaoBit.odontocore.crm.core.enums.TicketStatus;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.request.contactLog.ContactLogCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.api.dto.response.ContactLogResponseDTO;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.ContactLog;
import io.sertaoBit.odontocore.crm.modules.funnel.domain.model.Customer;
import io.sertaoBit.odontocore.crm.modules.funnel.mapper.ContactLogMapper;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.ContactLogRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.LeadTicketRepository;
import io.sertaoBit.odontocore.crm.modules.funnel.service.impl.ContactLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.ContactChannel.FACEBOOK;
import static io.sertaoBit.odontocore.crm.core.enums.TicketStatus.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactLogService - Testes Unitários do Serviço")
class ContactLogServiceTest {

    private  ContactLogService contactLogService;

    @Mock
    private ContactLogRepository contactLogRepository;

    @Mock
    private  ContactLogMapper contactLogMapper;

    @Mock
    private LeadTicketRepository leadTicketRepository;

    @Mock
    private SecurityUtils securityUtils;


    @BeforeEach
    void setUp() {

        contactLogService = new ContactLogServiceImpl(
                contactLogRepository,
                contactLogMapper,
                leadTicketRepository,
                securityUtils
        );
    }

    @Test
    @DisplayName("Deve cria um contactLog com sucesso")
    void create() {
        //Arrange
        UUID userId = UUID.randomUUID();
        securityUtils.getCurrentUser().setId(userId);
        UUID ticketId = UUID.randomUUID();


        ContactLogCreateRequestDTO dto = new ContactLogCreateRequestDTO(
             ticketId,
                FACEBOOK,"Cliente atingindo pela promoção de junho",
                LocalDateTime.now()
        );

        ContactLog contactLog = ContactLog.builder()
                .id(UUID.randomUUID())
                .ticketId(dto.ticketId())
                .userId(userId)
                .channel(dto.channel())
                .note(dto.note())
                .statusAfter(NEW)
                .createdAt(LocalDateTime.now())
                .occurredAt(dto.occurredAt())
                .build();


        when(contactLogRepository.save(any(ContactLog.class))).thenReturn(contactLog);


        ContactLogResponseDTO  expectedDTO = new ContactLogResponseDTO(
                contactLog.getId(),
                contactLog.getTicketId(),
                contactLog.getUserId(),
                contactLog.getChannel(),
                contactLog.getNote(),
                null,
                contactLog.getStatusAfter(),
                contactLog.getOccurredAt(),
                null

        );


        when(contactLogMapper.toResponseDTO(any(ContactLog.class))).thenReturn(expectedDTO);

    }


}