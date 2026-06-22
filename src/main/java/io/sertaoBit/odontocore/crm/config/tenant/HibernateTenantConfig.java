package io.sertaoBit.odontocore.crm.config.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateTenantConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantResolveCustomizer(ClinicResolveTenant resolve) {
        return properties ->
                properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolve);
        }
    }



