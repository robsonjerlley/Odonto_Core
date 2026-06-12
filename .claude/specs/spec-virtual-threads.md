# Spec: Virtual Threads (Java 21 + Spring Boot 3.2+)

**Status**: Backlog — aguardando conclusão das correções de backend
**Data**: 2026-06-11
**Autores**: Arquiteto-Agent
**Pré-requisito**: Java 21 (já em uso no projeto), Spring Boot 3.2+ (verificar versão atual)
**ADR de referência**: nenhum — decisão de infraestrutura, não de domínio

---

## O que são Virtual Threads e por que importam aqui

Virtual threads (Project Loom) são threads gerenciadas pela JVM, não pelo SO. A diferença prática:

| | Platform Thread (atual) | Virtual Thread |
|---|---|---|
| Custo de criação | ~1MB de stack reservado no SO | Bytes — criadas por milhares |
| Bloqueio em I/O | Segura thread do pool de Tomcat | Libera a carrier thread; a VT fica suspensa |
| Throughput sob carga I/O | Limitado pelo pool (ex: 200 threads Tomcat) | Praticamente ilimitado para I/O-bound |
| Mudança de código | — | Nenhuma (transparente para Spring MVC) |

O OdontoCore CRM é **100% I/O-bound**: todo request faz ao menos 1 query JDBC + 1 lookup de permissão. Virtual threads eliminam o gargalo de pool de threads sem refatoração.

---

## Impacto Esperado

- Requests simultâneos com espera de JDBC não bloqueiam threads do pool de Tomcat.
- `RecycleJob` (`@Scheduled`) não compete por thread com requests HTTP.
- `PermissionService` (1 query por `checkOrThrow`) se beneficia diretamente: a thread suspende no JDBC e libera a carrier thread imediatamente.
- Sem impacto em lógica de negócio — virtual threads são transparentes para o código de aplicação.

---

## Implementação

### Passo 1 — Verificar versão do Spring Boot

Virtual threads com suporte nativo do Spring Boot exigem **3.2+**:

```bash
# em pom.xml, verificar:
<parent>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.x</version>  <!-- ou superior -->
</parent>
```

Se a versão for inferior a 3.2, atualizar `spring-boot-starter-parent` primeiro.

### Passo 2 — Habilitar via `application.properties`

```properties
# application.properties (produção e dev)
spring.threads.virtual.enabled=true
```

Uma linha. Isso configura:
- Tomcat para usar `VirtualThreadExecutor` no lugar do pool fixo
- `@Async` para usar virtual threads automaticamente
- `@Scheduled` para usar virtual threads

**Não remover nem alterar** `server.tomcat.threads.max` se existir — com virtual threads, o limite de conexões simultâneas passa a ser controlado pela configuração do servidor de banco (HikariCP), não do Tomcat.

### Passo 3 — Ajustar HikariCP

Virtual threads não bloqueiam carrier threads, mas **conexões de banco são recursos escassos**. Com virtual threads, muito mais requests podem atingir o banco simultaneamente — o pool de conexões se torna o novo gargalo.

Adicionar ao `application.properties`:

```properties
# Tamanho do pool de conexões HikariCP
# Fórmula recomendada para single-core/small instance: (núcleos × 2) + spindle_disks
# Para Railway (2 vCPU, PostgreSQL compartilhado):
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

⚠️ **Não aumentar `maximum-pool-size` além do que o plano do PostgreSQL suporta.** Railway Free tier tem limite de conexões simultâneas. Verificar no dashboard do Railway antes de subir.

### Passo 4 — Remover configuração de pool de Tomcat (se existir)

Com virtual threads, `server.tomcat.threads.max` e `server.tomcat.threads.min-spare` perdem sentido. Remover se presentes para não gerar confusão:

```properties
# REMOVER se existir:
# server.tomcat.threads.max=200
# server.tomcat.threads.min-spare=10
```

---

## O que NÃO muda

- Nenhum arquivo Java de domínio, serviço ou controller.
- `@Transactional` funciona normalmente com virtual threads.
- `ThreadLocal` funciona normalmente (Spring Security usa `SecurityContextHolder` com `ThreadLocal` — compatível desde Spring 6 / Boot 3.2).
- `SecurityUtils.getCurrentUser()` (que usa `SecurityContextHolder`) não muda.

---

## Validação Pós-Deploy

Verificar no log de startup que o Tomcat informa uso de virtual threads:

```
o.s.b.w.e.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
```

Para confirmar virtual threads ativos, adicionar temporariamente ao startup:

```java
log.info("Virtual threads enabled: {}", Thread.currentThread().isVirtual());
```

Remover após confirmar.

---

## Riscos e Mitigações

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Pool de conexões esgotado sob carga | Média | Ajustar `maximum-pool-size` conforme plano do Railway |
| Bibliotecas que usam `synchronized` com I/O (pinning) | Baixa | Spring e HikariCP 5.x são compatíveis; verificar libs de terceiros se adicionadas |
| Comportamento diferente em testes | Baixa | Testes de integração com `@SpringBootTest` executam normalmente |

---

## Referências

- [Spring Boot 3.2 Virtual Threads](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/web.html#web.servlet.embedded-container.tomcat.threads)
- [HikariCP sizing guide](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- Java 21 JEP 444 (Virtual Threads — GA)
