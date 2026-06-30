package io.sertaoBit.odontocore.crm.modules.financial.repository;

import io.sertaoBit.odontocore.crm.config.tenant.ClinicResolveTenant;
import io.sertaoBit.odontocore.crm.config.tenant.HibernateTenantConfig;
import io.sertaoBit.odontocore.crm.config.tenant.TenantContext;
import io.sertaoBit.odontocore.crm.core.enums.PaymentStatus;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.EXPECTED;
import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.PAID;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ClinicResolveTenant.class, HibernateTenantConfig.class})
@DisplayName("InstallmentRepository.cashflow - JPQL de agregação (Testcontainers)")
class InstallmentRepositoryCashflowTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("odontocoredb")
            .withUsername("postgres")
            .withPassword("odontodb123");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Hibernate cria o schema crm_db referenciado pela entity (sem schema.sql/Flyway)
        registry.add("spring.jpa.properties.jakarta.persistence.create-database-schemas", () -> "true");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    private static final UUID CLINIC = UUID.randomUUID();

    @Autowired
    private InstallmentRepository repository;

    @BeforeEach
    void setTenant() {
        TenantContext.setCurrent(CLINIC);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private Installment installment(int year, int month, PaymentStatus status,
                                    String expected, String paid) {
        return Installment.builder()
                .dealId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerName("Cliente")
                .sequence(1)
                .totalInstallments(1)
                .dueDate(LocalDate.of(year, month, 10))
                .expectedAmount(new BigDecimal(expected))
                .status(status)
                .paidAmount(paid == null ? null : new BigDecimal(paid))
                .build();
    }

    @Test
    @DisplayName("agrupa por ano/mês e soma recebido (PAID) e aReceber (EXPECTED)")
    void cashflow_aggregatesByMonthAndStatus() {
        repository.saveAll(List.of(
                installment(2026, 6, PAID, "100", "100"),
                installment(2026, 6, EXPECTED, "200", null),
                installment(2026, 7, PAID, "50", "50")
        ));
        repository.flush();

        List<CashflowRow> rows = repository.cashflow(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31), PAID, EXPECTED);

        assertEquals(2, rows.size());

        CashflowRow june = rows.get(0);
        assertEquals(2026, june.year());
        assertEquals(6, june.month());
        assertEquals(0, new BigDecimal("100").compareTo(june.recebido()));
        assertEquals(0, new BigDecimal("200").compareTo(june.aReceber()));

        CashflowRow july = rows.get(1);
        assertEquals(7, july.month());
        assertEquals(0, new BigDecimal("50").compareTo(july.recebido()));
        assertEquals(0, BigDecimal.ZERO.compareTo(july.aReceber()));
    }

    @Test
    @DisplayName("janela de datas exclui parcelas fora do intervalo")
    void cashflow_respectsDateWindow() {
        repository.saveAll(List.of(
                installment(2026, 5, PAID, "999", "999"),
                installment(2026, 6, PAID, "100", "100")
        ));
        repository.flush();

        List<CashflowRow> rows = repository.cashflow(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), PAID, EXPECTED);

        assertEquals(1, rows.size());
        assertEquals(6, rows.get(0).month());
        assertEquals(0, new BigDecimal("100").compareTo(rows.get(0).recebido()));
    }
}
