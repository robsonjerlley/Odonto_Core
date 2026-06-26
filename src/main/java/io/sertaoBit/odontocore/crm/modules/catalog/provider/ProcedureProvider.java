package io.sertaoBit.odontocore.crm.modules.catalog.provider;

import java.util.List;
import java.util.UUID;

public interface ProcedureProvider {

    List<ProcedureView> resolveActiveByIds(List<UUID> ids);
}
