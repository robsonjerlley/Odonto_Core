package io.sertaoBit.odontocore.crm.config.tenant;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    public static void setCurrent(UUID clinicId) {
        CURRENT.set(clinicId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    private TenantContext() {}
}
