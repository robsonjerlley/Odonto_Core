package io.sertaoBit.odontocore.crm.modules.catalog.service;

import io.sertaoBit.odontocore.crm.modules.catalog.api.dto.response.ProcedureView;

import java.util.List;
import java.util.UUID;

public interface ProcedureProvider {

    List<ProcedureView> resolveActiveByIds(List<UUID> ids);
}
