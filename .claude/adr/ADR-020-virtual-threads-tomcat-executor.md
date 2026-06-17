# ADR-020: Virtual Threads como executor do Tomcat

**Status**: Implementado  
**Data**: 2026-06-17  
**Autores**: Arquiteto-Agent  
**Impacto**: Infraestrutura — `application.properties`, `application-prod.properties`, `nixpacks.toml`

---

## Contexto

O OdontoCore CRM é 100% I/O-bound: todo request HTTP faz ao menos uma query JDBC (dados de negócio) mais uma query de permissão (`checkOrThrow`). Com o modelo de platform threads do Tomcat, cada request segura uma thread do pool enquanto aguarda resposta do banco — sob carga, o pool de 200 threads se torna o gargalo.

O projeto já roda Java 21 e Spring Boot 4.0.5, que suportam Virtual Threads nativamente via `spring.threads.virtual.enabled=true`.

Simultaneamente, o plano de infraestrutura no Railway foi atualizado de 1 vCPU / 1 GB RAM para **8 vCPU / 8 GB RAM**, exigindo revisão das flags de JVM e do pool de conexões HikariCP.

---

## Decisão

Habilitar Virtual Threads no Tomcat via propriedade do Spring Boot e redimensionar JVM e HikariCP para o novo hardware.

### Mudanças aplicadas

**`application.properties`**
```properties
spring.threads.virtual.enabled=true
```

**`application-prod.properties` — HikariCP**
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

Pool calculado pela fórmula HikariCP para I/O-bound em SSD/cloud:
`(vCPU × 2) + spindle_count = (8 × 2) + 1 = 17 → 20`

**`nixpacks.toml` — JVM flags**
```
-Xmx6g -XX:MaxMetaspaceSize=512m -XX:+ExitOnOutOfMemoryError
```

Cálculo de heap: 8 GB − ~1.5 GB (SO + Metaspace + JIT + direct buffers + 8 carrier threads) = 6.5 GB disponíveis → `-Xmx6g` como teto seguro.

---

## Por que não `@Async` + `CompletableFuture`

A abordagem de anotar métodos individualmente (ex.: `search()`) foi avaliada e descartada:

| Problema | Consequência |
|---|---|
| `@Async` cria nova thread separada do request | `@Transactional` não propaga — contexto JPA perde a sessão |
| `SecurityContextHolder` não herda para thread async | `SecurityUtils.getCurrentUser()` retorna `null` |
| `.join()` bloqueia a thread chamadora | Usa 2 threads onde 1 resolve — overhead sem benefício |
| Mudança de retorno para `CompletableFuture<T>` | Quebra contrato de todos os controllers |

`spring.threads.virtual.enabled=true` aplica a mudança na entrada do request — o método `search()` já está em uma virtual thread sem nenhuma anotação.

---

## Consequências positivas

- Requests simultâneos com espera de JDBC não bloqueiam carrier threads do pool.
- `RecycleJob` (`@Scheduled`) não compete por thread com requests HTTP.
- `PermissionService.checkOrThrow()` (1 query por chamada) se beneficia em todo request.
- Zero mudança em código de domínio, serviços ou controllers.
- `@Transactional` e `SecurityContextHolder` continuam funcionando normalmente.

## Consequências negativas / riscos

| Risco | Mitigação |
|---|---|
| Pool de conexões esgotado sob alta concorrência | HikariCP dimensionado para 20; conexões são o regulador natural do fluxo |
| Bibliotecas com `synchronized` em I/O (thread pinning) | Spring 6 e HikariCP 5.x são compatíveis; monitorar se libs externas forem adicionadas |
| `-Xmx6g` pode ser insuficiente se houver leak real | `-XX:+ExitOnOutOfMemoryError` garante restart limpo; Railway reinicia automaticamente |

---

## Alternativas consideradas

- **`@Async` por método**: descartado — quebra transação e contexto de segurança (ver seção acima).
- **Manter platform threads**: descartado — sem custo de migração para Virtual Threads e ganho imediato em throughput I/O-bound.
- **Aumentar pool Tomcat (`server.tomcat.threads.max`)**: descartado — solução linear que não escala; Virtual Threads resolvem a causa raiz.