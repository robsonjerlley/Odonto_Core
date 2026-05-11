package io.sertaoBit.odontocore.crm.modules.commercial.service;

import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.ApplyDiscountRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealCreateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.request.deal.DealUpdateRequestDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.api.dto.response.DealDetailResponseDTO;
import io.sertaoBit.odontocore.crm.modules.commercial.model.Deal;

import java.util.UUID;


public interface DealService {

    Deal create(UUID ticketId, DealCreateRequestDTO dto);

    Deal update(UUID dealId, DealUpdateRequestDTO dto);

    Deal applyDiscount(UUID dealId, ApplyDiscountRequestDTO dto);

    Deal closeDeal(UUID dealId, String paymentMethod);

    DealDetailResponseDTO getDealWithHistory(UUID dealId);
}


