package io.sertaoBit.odontocore.crm.modules.funnel.provider;

import java.util.UUID;

public interface CustomerProvider {

    CustomerView resolveById(UUID id);

}
