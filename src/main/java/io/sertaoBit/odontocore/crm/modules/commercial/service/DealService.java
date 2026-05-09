package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;


import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;

import java.math.BigDecimal;
import java.util.UUID;


public interface DealService {

    Deal create(UUID ticketId, DealCreateRequestDTO dto, User by);

    Deal update(UUID dealId, DealUpdateRequestDTO dto, User by);

    Deal applyDiscount(UUID dealId, BigDecimal pct, User by);

    Deal closeDeal(UUID dealId, String paymentMethod, User by);

    DealDetailResponseDTO getDealWithHistory(UUID dealId, User by);
}


