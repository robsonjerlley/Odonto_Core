package io.sertaoBit.odontocore.crm;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Classe base para testes que requerem PostgreSQL via Testcontainers.
 * Requer Docker instalado e rodando.
 * Para rodar testes: mvn test (com Docker)
 * Para build sem testes: mvn clean package -DskipTests
 */
@Testcontainers
@SpringBootTest
public abstract class AbstractTestcontainerTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("odontocoredb")
			.withUsername("postgres")
			.withPassword("odontodb123");

	@DynamicPropertySource
	static void registerPgProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}
}
