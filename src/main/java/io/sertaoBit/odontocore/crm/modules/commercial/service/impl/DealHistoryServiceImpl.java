package io.sertaoBit.odontocore.crm.modules.commercial.service.impl;


import tools.jackson.databind.ObjectMapper;
import io.sertaoBit.odontocore.crm.modules.commercial.model.DealHistory;
import io.sertaoBit.odontocore.crm.modules.commercial.repository.DealHistoryRepository;
import io.sertaoBit.odontocore.crm.modules.commercial.service.DealHistoryService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DealHistoryServiceImpl implements DealHistoryService {

    private final DealHistoryRepository dealHistoryRepository;
    private final ObjectMapper objectMapper;

    public DealHistoryServiceImpl(DealHistoryRepository dealHistoryRepository, ObjectMapper objectMapper) {
        this.dealHistoryRepository = dealHistoryRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void record(UUID dealId, User user, Object before, Object after) {
        DealHistory history = DealHistory.builder()
                .dealId(dealId)
                .valueBefore(objectMapper.writeValueAsString(before))
                .valueAfter(objectMapper.writeValueAsString(after))
                .occurredAt(LocalDateTime.now())
                .build();
        dealHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealHistory> findByDealId(UUID dealId) {
        return dealHistoryRepository.findByDealIdOrderByOccurredAt(dealId);
    }
}