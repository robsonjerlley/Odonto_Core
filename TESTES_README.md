# 🧪 Testes Automáticos - OdontoCore CRM

## 📋 Sumário

Este documento descreve a estratégia de testes automáticos para o módulo CRM da aplicação OdontoCore.

### ✅ Testes Criados

1. **CustomerServiceTest.java** (5 test cases)
   - CREATE: testar criação com sucesso, validações de department
   - FIND: buscar por ID, por CPF
   - UPDATE: atualizar com validação de CPF duplicado
   - DELETE: deletar com validação de existência

2. **TicketServiceTest.java** (6 test cases)
   - CREATE: testar criação validando customer e user
   - FIND: buscar por ID, por status, por customer
   - UPDATE: atualizar status
   - DELETE: deletar com validação

3. **ContactLogServiceTest.java** (8 test cases)
   - CREATE: criar com validações de customer, ticket, user
   - FIND: buscar por ID, por customer, por canal, por outcome
   - UPDATE: atualizar campos
   - DELETE: deletar
   - SPECIAL: buscar com follow-up pendente

4. **DealServiceTest.java** (7 test cases)
   - CREATE: criar com validação de customer
   - FIND: buscar por ID, por status, por customer, por data range
   - UPDATE: atualizar status
   - DELETE: deletar

5. **CustomerControllerTest.java** (8 test cases)
   - POST /create: criar e retornar 201
   - GET /: listar todos (200)
   - GET /{id}: buscar por ID (200)
   - GET /cpf/{cpf}: buscar por CPF (200)
   - PATCH /update/{cpf}: atualizar (200)
   - DELETE /{id}: deletar (204)
   - Validação: erro 400 para entrada inválida
   - Erro: 404/500 para recurso não encontrado

---

## 🚀 Como Executar

### Executar Todos os Testes
```bash
mvn test
```

### Executar Teste Específico
```bash
mvn test -Dtest=CustomerServiceTest
```

### Executar Classe de Teste Específica
```bash
mvn test -Dtest=CustomerServiceTest#testCreateCustomerSuccess
```

### Executar com Cobertura
```bash
mvn clean test jacoco:report
# Relatório gerado em: target/site/jacoco/index.html
```

### Executar em Modo Debug
```bash
mvn test -Dtest=CustomerServiceTest -DforkCount=0
```

---

## 📊 Estrutura de Testes

### Padrão Utilizado: AAA (Arrange-Act-Assert)

```java
@Test
@DisplayName("Descrição do teste")
void testMethod() {
    // ARRANGE - Preparar dados e mocks
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    // ACT - Executar a ação
    ResultDTO result = service.findById(id);

    // ASSERT - Validar resultado
    assertNotNull(result);
    assertEquals(id, result.id());
    verify(repository, times(1)).findById(id);
}
```

### Mocking Framework: Mockito
- `@Mock` para dependências
- `@ExtendWith(MockitoExtension.class)` para ativar Mockito
- `when()...thenReturn()` para configurar comportamento
- `verify()` para validar chamadas

### Controller Testing: Spring Test
- `@WebMvcTest(ControllerClass.class)` para testes isolados
- `MockMvc` para fazer requisições HTTP
- `mockMvc.perform()` para executar requisições
- Validação de status HTTP, headers, body JSON

---

## 🎯 Cobertura de Testes por Componente

### Services (Unitários)
| Serviço | Métodos | Testes | Coverage |
|---------|---------|--------|----------|
| CustomerService | 7 | 5 | 71% |
| TicketService | 9 | 6 | 67% |
| ContactLogService | 11 | 8 | 73% |
| DealService | 9 | 7 | 78% |
| **TOTAL** | **36** | **26** | **72%** |

### Controllers (Integração)
| Controller | Endpoints | Testes | Coverage |
|------------|-----------|--------|----------|
| CustomerController | 7 | 8 | 100% |
| **TODO** | | |
| TicketController | 9 | - | 0% |
| ContactLogController | 11 | - | 0% |
| DealController | 9 | - | 0% |
| SalesMetricsController | 15 | - | 0% |

---

## 🔍 Exemplos de Teste

### Exemplo 1: Teste de Serviço (Unitário)
```java
@Test
@DisplayName("Deve criar customer com sucesso")
void testCreateCustomerSuccess() {
    // ARRANGE
    Department dept = new Department();
    dept.setId(departmentId);

    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setName("João");

    when(departmentRepository.findById(departmentId))
        .thenReturn(Optional.of(dept));
    when(customerRepository.save(any()))
        .thenReturn(customer);
    when(customerMapper.toResponseDTO(customer))
        .thenReturn(new CustomerResponseDTO(...));

    // ACT
    CustomerResponseDTO result = customerService.create(dto);

    // ASSERT
    assertNotNull(result);
    assertEquals("João", result.name());
    verify(departmentRepository).findById(departmentId);
}
```

### Exemplo 2: Teste de Controller (Integração)
```java
@Test
@DisplayName("GET /api/v1/customers - Deve retornar lista")
void testFindAll() throws Exception {
    // ARRANGE
    List<CustomerResponseDTO> dtos = List.of(...);
    when(service.findAll()).thenReturn(dtos);

    // ACT & ASSERT
    mockMvc.perform(get("/api/v1/customers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("João"));
}
```

---

## 🛠️ Dependências de Teste

```xml
<!-- Em pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

---

## ⚙️ Configuração (application-test.properties)

```properties
# Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Logging
logging.level.root=WARN
logging.level.io.sertaoBit.odontocore=DEBUG

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
```

---

## 📈 Próximos Passos - Testes Faltando

- [ ] TicketControllerTest.java
- [ ] ContactLogControllerTest.java
- [ ] DealControllerTest.java
- [ ] SalesMetricsControllerTest.java
- [ ] ContactLogMapperTest.java
- [ ] TicketMapperTest.java
- [ ] DealMapperTest.java
- [ ] SalesMetricsServiceTest.java
- [ ] Testes de Integração E2E (E2E tests)
- [ ] Testes de Performance (load tests)

---

## 🐛 Troubleshooting

### Erro: "Cannot find @SpringBootConfiguration"
**Solução**: Adicione `@SpringBootTest` ou `@WebMvcTest` na classe de teste

### Erro: "No qualifying bean of type..."
**Solução**: Use `@MockBean` para dependências não gerenciadas

### Erro: "Test throws runtime exception"
**Solução**: Verifique se o mock foi configurado corretamente com `when()`

### Erro: "Expected 1, but was 0" (verify failed)
**Solução**: Verifique se o método foi realmente chamado no código testado

---

## 📚 Referências

- [Spring Test Documentation](https://spring.io/guides/gs/testing-web/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

---

## 📞 Contato

Para dúvidas sobre testes, consulte a documentação do Spring Boot Test.

