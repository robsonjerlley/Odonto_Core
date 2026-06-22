package io.sertaoBit.odontocore.crm.config.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClinicResolveTenant implements CurrentTenantIdentifierResolver<UUID> {
    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.get();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
