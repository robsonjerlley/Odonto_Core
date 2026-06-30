package io.sertaoBit.odontocore.crm.modules.commercial.provider;

import java.util.UUID;

public interface DealFinancialProvider {

    DealFinancialView resolveById(UUID dealId);
}
