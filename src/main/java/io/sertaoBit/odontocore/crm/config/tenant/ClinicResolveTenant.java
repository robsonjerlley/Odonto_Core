package io.sertaoBit.odontocore.crm.config.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClinicResolveTenant implements CurrentTenantIdentifierResolver<UUID> {

    // Sentinel usado fora de request (startup, jobs sem TenantContext).
    // Entidades com @TenantId filtram por esse UUID → resultado vazio, sem vazamento.
    private static final UUID NO_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID current = TenantContext.get();
        return current != null ? current : NO_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
