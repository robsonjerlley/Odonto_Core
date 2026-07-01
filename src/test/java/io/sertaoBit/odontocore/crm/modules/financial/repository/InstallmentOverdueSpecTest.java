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
import static io.sertaoBit.odontocore.crm.modules.financial.repository.InstallmentSpecifications.overdue;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ClinicResolveTenant.class, HibernateTenantConfig.class})
@DisplayName("InstallmentSpecifications.overdue - EXPECTED vencida, cross-month (Testcontainers)")
class InstallmentOverdueSpecTest {

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

    private Installment installment(LocalDate dueDate, PaymentStatus status) {
        return Installment.builder()
                .dealId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerName("Cliente")
                .sequence(1)
                .totalInstallments(1)
                .dueDate(dueDate)
                .expectedAmount(new BigDecimal("100.00"))
                .status(status)
                .paidAmount(status == PAID ? new BigDecimal("100.00") : null)
                .build();
    }

    @Test
    @DisplayName("retorna só EXPECTED vencidas, de meses diferentes; ignora futura e PAID")
    void overdue_returnsOnlyPastExpected_acrossMonths() {
        LocalDate today = LocalDate.now();
        Installment overduePrevMonth = installment(today.minusMonths(2).withDayOfMonth(10), EXPECTED);
        Installment overdueThisMonth = installment(today.minusDays(1), EXPECTED);
        Installment futureExpected = installment(today.plusDays(5), EXPECTED);   // vencida no futuro → não
        Installment paidPast = installment(today.minusDays(3), PAID);            // já paga → não

        repository.saveAll(List.of(overduePrevMonth, overdueThisMonth, futureExpected, paidPast));
        repository.flush();

        List<Installment> result = repository.findAll(overdue());

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(i -> i.getStatus() == EXPECTED));
        assertTrue(result.stream().allMatch(i -> i.getDueDate().isBefore(today)));
        // cross-month: os dois atrasados caem em meses distintos
        assertEquals(2, result.stream().map(i -> i.getDueDate().getMonth()).distinct().count());
    }

    @Test
    @DisplayName("sem parcelas atrasadas retorna lista vazia")
    void overdue_emptyWhenNoneOverdue() {
        repository.saveAll(List.of(
                installment(LocalDate.now().plusDays(10), EXPECTED),
                installment(LocalDate.now().minusDays(10), PAID)
        ));
        repository.flush();

        assertTrue(repository.findAll(overdue()).isEmpty());
    }
}
