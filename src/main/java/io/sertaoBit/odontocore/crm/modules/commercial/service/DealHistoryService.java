package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.modules.commercial.model.DealHistory;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;

import java.util.List;
import java.util.UUID;

public interface DealHistoryService {

    void record(
            UUID dealId,User user,
            Object before, Object after
    );

    List<DealHistory> findByDealId(UUID dealId);
}