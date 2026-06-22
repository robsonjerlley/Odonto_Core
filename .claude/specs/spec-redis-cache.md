# Spec: Cache com Redis

**Status**: Backlog — aguardando implementação de ADR-022 (clinicId no User + JWT)
**Data**: 2026-06-11 (atualizado 2026-06-22)
**Autores**: Arquiteto-Agent
**Pré-requisito**: ADR-015 implementada (analytics scope-aware) ✅ + **ADR-022 implementada (clinicId no User/JWT)** + ADR-024 (enforcement `@TenantId`)
**ADR de referência**: ADR-014 (Flyway), ADR-022 (multi-tenancy), ADR-024 (`@TenantId` enforcement), ADR-023 (TicketWonEvent)

> **Atualização 2026-06-17**: Estratégia de cache revista para suportar múltiplas clínicas.
> Todas as chaves de cache de analytics e configs novos **devem incluir `clinicId`** como
> primeiro segmento após o namespace. Isso permite evicção cirúrgica por clínica
> (`analytics:{clinicId}:*`) sem afetar dados de outras clínicas.
> A implementação dos módulos Financeiro e Consultas (ADR-023) cria novos caches —
> veja seção "Módulos novos" ao final.

---

> ## 🚫 Atualização 2026-06-22 — o `@TenantId` (ADR-024) NÃO cobre o Redis
>
> **Armadilha crítica.** A ADR-024 fez o Hibernate isolar o **banco** automaticamente (`@TenantId`
> injeta `WHERE clinic_id = ?`). É fácil concluir, erradamente, que "o tenant agora é automático" e
> relaxar a chave do cache. **O Redis é um keyspace global, cego a tenant.** O `@Cacheable` grava
> sob a chave que você definir — se faltar `clinicId`, há vazamento entre clínicas **mesmo com o
> banco perfeitamente isolado**:
>
> ```java
> // 🚫 CILADA: banco isolado pelo @TenantId, mas cache vaza
> @Cacheable(value = "analytics", key = "'revenue:' + #from + ':' + #to")  // sem clinicId!
> public RevenueResultDTO getRevenue(LocalDate from, LocalDate to) { ... }
> // Clínica A popula 'revenue:2026:...'; Clínica B chama com as mesmas datas → HIT → dados de A
> ```
>
> **Regras invioláveis (independentes da ADR-024):**
> 1. Todo método cacheado multi-tenant recebe `clinicId` como **parâmetro explícito** e o usa na
>    chave via SpEL (`#clinicId`). Não dependa do `TenantContext` para compor a chave — funciona,
>    mas fica frágil/implícito.
> 2. `clinicId` é o **primeiro segmento** após o namespace → evicção cirúrgica `analytics:{clinicId}:*`.
> 3. Invalidação em listener async usa `event.getClinicId()` do payload (não o `SecurityContext`).
>
> O `@TenantId` protege o banco; **a disciplina da chave protege o cache.** São camadas distintas.

---

## Motivação

O OdontoCore CRM tem três categorias de dados com excelente perfil para cache:

| Categoria | Frequência de leitura | Frequência de escrita | Custo de recalcular |
|---|---|---|---|
| Regras de permissão (`PermissionRule`) | Toda request (1+ queries) | Apenas no seeder (raro) | Baixo, mas acumulado |
| Analytics agregados | Alta (dashboards) | Contínua (dados mudam) | Alto (JOINs, COUNT, GROUP BY) |
| Configurações (`RecycleConfig`, `BonusConfig`, `AdsInvestment`) | Toda operação de regra de negócio | Raramente | Baixo |

Sem cache: cada `checkOrThrow()` dispara 1–2 queries ao banco para carregar `PermissionRule`. Em alta concorrência (+ virtual threads), isso multiplica rapidamente.

---

## Arquitetura de Cache

```
[Request] → [Spring Security Filter] → [Service]
                                           ↓
                                    [CacheManager (Redis)]
                                       hit ↙     ↘ miss
                               [retorna cache]  [query DB → salva cache]
```

Spring Cache Abstraction (`@Cacheable`, `@CacheEvict`) é a camada de integração — os serviços não conhecem Redis diretamente. Trocar Redis por outro provider no futuro exige apenas mudar configuração.

---

## Dependências

Adicionar ao `pom.xml`:

```xml
<!-- Redis client + Spring Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Spring Cache Abstraction -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

---

## Configuração

### `application.properties`

```properties
# Redis connection (Railway Redis add-on ou local)
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.ssl.enabled=${REDIS_SSL:false}

# TTL padrão global (pode ser sobrescrito por cache nomeado)
spring.cache.redis.time-to-live=300000
# Serialização JSON (não Java serialization — compatível com evolução de classes)
spring.cache.redis.use-key-prefix=true
spring.cache.redis.key-prefix=odontocore:
```

### `application-local.properties`

```properties
# Redis local (Docker): docker run -p 6379:6379 redis:alpine
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.ssl.enabled=false
```

### `CacheConfig.java`

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                .prefixCacheNameWith("odontocore:");

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "permissionRules",   defaultConfig.entryTtl(Duration.ofHours(24)),
                "analytics",         defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "configs",           defaultConfig.entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
```

---

## Implementação por Camada

### 1. Cache de Regras de Permissão (`permissionRules`)

**Arquivo**: `PermissionService.java`

**Estratégia**: cachear o resultado de `getScope()` e `checkOrThrow()` por `(role, sector, resource, action)`. Como o seeder cria as regras uma única vez, o TTL pode ser longo (24h). O `@CacheEvict` é chamado quando o seeder recarrega as regras.

```java
// Em PermissionService (ou no PermissionRuleRepository):

@Cacheable(value = "permissionRules",
           key = "#role + ':' + #sector + ':' + #resource + ':' + #action")
public Optional<PermissionRule> findRule(Role role, Sector sector, Resource resource, Action action) {
    return ruleRepository.findByRoleAndSectorAndResourceAndAction(role, sector, resource, action)
            .or(() -> ruleRepository.findByRoleAndResourceAndAction(role, resource, action));
}
```

**Invalidação no seeder** (`PermissionSeeder.java`):

```java
@CacheEvict(value = "permissionRules", allEntries = true)
public void seed() {
    // ... lógica atual
}
```

⚠️ **Atenção**: o seeder tem `if (count() > 0) return` — com cache quente, o `@CacheEvict` só dispara se o método for executado. Para forçar invalidação após reset manual do banco, adicionar endpoint ADM_SYSTEM:

```java
// UserController ou AdminController
@PostMapping("/admin/cache/evict")
public ResponseEntity<Void> evictAllCaches(@AuthenticationPrincipal ...) {
    // checkOrThrow ADM_SYSTEM
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    return ResponseEntity.noContent().build();
}
```

---

### 2. Cache de Analytics (`analytics`)

**Arquivo**: `AnalyticsServiceImpl.java`

**Estratégia**: TTL curto (5 min) — dados mudam continuamente mas dashboards não precisam de realtime. A chave inclui o escopo + filtros de data para não misturar recortes.

```java
@Cacheable(value = "analytics",
           key = "'dashboard:' + #scope + ':' + #sector + ':' + #from + ':' + #to")
public DashboardDTO getDashboard(PermissionScope scope, Sector sector,
                                  LocalDate from, LocalDate to) {
    // query existente
}

@Cacheable(value = "analytics",
           key = "'ads-roi:' + #scope + ':' + #sector + ':' + #from + ':' + #to")
public AdsRoiDTO getAdsRoi(PermissionScope scope, Sector sector,
                            LocalDate from, LocalDate to) {
    // query existente
}
```

**Pré-condição**: ADR-015 deve estar implementada antes — o `scope` precisa estar no parâmetro para compor a chave de cache corretamente.

**Invalidação**: não há — o TTL de 5 min é suficiente para dashboards operacionais. Se necessário no futuro, um endpoint `/admin/cache/evict` já cobre.

---

### 3. Cache de Configurações (`configs`)

**Arquivo**: `ConfigServiceImpl.java`

**Estratégia**: TTL de 1h — `RecycleConfig`, `BonusConfig` e `AdsInvestment` mudam apenas por ação explícita de `ADM_SYSTEM`.

```java
@Cacheable(value = "configs", key = "'recycle-config'")
public RecycleConfigResponseDTO getRecycleConfig() {
    // query existente
}

@Cacheable(value = "configs", key = "'bonus-config'")
public BonusConfigResponseDTO getBonusConfig() {
    // query existente
}

@Cacheable(value = "configs", key = "'ads-investment:' + #year + ':' + #month")
public AdsInvestmentResponseDTO getAdsInvestment(int year, int month) {
    // query existente
}
```

**Invalidação nos métodos de update** (quando implementados):

```java
@CacheEvict(value = "configs", key = "'recycle-config'")
public RecycleConfigResponseDTO updateRecycleConfig(...) { ... }
```

---

## Ordem de Implementação

```
1. pom.xml                    → adicionar dependências Redis + Cache
2. application.properties     → conexão Redis + TTLs
3. CacheConfig.java           → criar classe de configuração
4. PermissionService.java     → @Cacheable em findRule() + @CacheEvict no seeder
5. ConfigServiceImpl.java     → @Cacheable nos 3 getters
6. AnalyticsServiceImpl.java  → @Cacheable (depende de ADR-015 implementada)
7. Endpoint /admin/cache/evict → opcional, para operação
```

---

## Infraestrutura (Railway)

Railway tem Redis como add-on nativo:

1. No dashboard do Railway: **New → Database → Redis**
2. As variáveis de ambiente `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` são injetadas automaticamente no serviço vinculado
3. `REDIS_SSL=true` é necessário para Redis na Railway em produção

---

## Riscos e Mitigações

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Cache stale de permissões após reset do seeder | Alta | `@CacheEvict` no seeder + endpoint de evict manual |
| Chave de cache duplicada entre escopos | Média | Incluir `scope` e `sector` na chave sempre |
| Redis indisponível (falha de conexão) | Baixa | Spring Cache degrada gracefully para DB se `spring.cache.redis.cache-null-values=false` e a exceção não for fatal |
| Serialização falha para tipos customizados | Baixa | `GenericJackson2JsonRedisSerializer` serializa qualquer POJO com Jackson; enums são strings |
| Over-caching de dados sensíveis (PII) | Baixa | Cache contém apenas DTOs de resposta — os mesmos já expostos via API |

---

## O que NÃO cachear

- **Entidades JPA completas** (`User`, `Customer`, `LeadTicket`) — usar apenas DTOs de resposta para evitar LazyInitializationException e vazar dados além do contrato
- **Tokens JWT** — stateless por design; cache introduziria estado que conflita com ADR-005
- **`Page<T>`** — paginação muda com cada insert/delete; cache de página específica cria inconsistência

---

## Módulos novos — caches financeiro e clínico (ADR-023)

Os módulos Financeiro e Consultas geram novos dados de analytics. Todo cache desses módulos
**obrigatoriamente inclui `clinicId`** como primeiro segmento da chave:

```java
// AnalyticsServiceImpl.java — métodos novos para módulos downstream

@Cacheable(value = "analytics",
           key = "'revenue:' + #clinicId + ':' + #from + ':' + #to")
public RevenueResultDTO getRevenueRealized(UUID clinicId, LocalDate from, LocalDate to) { }

@Cacheable(value = "analytics",
           key = "'treatment-rate:' + #clinicId + ':' + #from + ':' + #to")
public TreatmentRateResultDTO getTreatmentCompletionRate(UUID clinicId, LocalDate from, LocalDate to) { }
```

**Invalidação após escrita nos módulos downstream**: os listeners `FinancialEventListener` e
`ClinicalEventListener` (ADR-023) processam em `@Async AFTER_COMMIT`. A invalidação do cache
de analytics ocorre dentro do listener, por `clinicId`:

```java
// dentro do listener, após persistir o dado:
Cache analyticsCache = cacheManager.getCache("analytics");
// evicção cirúrgica: apenas entradas da clínica afetada
analyticsCache.evict("revenue:" + event.getClinicId() + ":...");
// ou, se múltiplas chaves: clear seletivo por prefixo via RedisTemplate
```

Para entradas de analytics dos módulos legados (tickets, deals) — TTL de 5 min permanece como
estratégia. Para dados dos novos módulos — Write-Invalidation por `clinicId` via listener.

---

## O que NÃO cachear

- **Entidades JPA completas** (`User`, `Customer`, `LeadTicket`) — usar apenas DTOs de resposta para evitar LazyInitializationException e vazar dados além do contrato
- **Tokens JWT** — stateless por design; cache introduziria estado que conflita com ADR-005
- **`Page<T>`** — paginação muda com cada insert/delete; cache de página específica cria inconsistência
- **Analytics sem `clinicId` na chave** (a partir de ADR-022) — cache compartilhado entre clínicas é bug de isolamento de dados

---

## Referências

- [Spring Boot Cache Abstraction](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.caching)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- `ADR-015` — analytics scope-aware (pré-requisito para cachear analytics corretamente)
- `ADR-022` — clinicId em User + JWT (pré-requisito para chaves de cache multi-tenant)
- `ADR-024` — enforcement `@TenantId` (isola o **banco**, NÃO o Redis — chave com `clinicId` continua obrigatória e manual)
- `ADR-025` — RLS PostgreSQL (defesa em profundidade futura; também não cobre o cache)
- `ADR-023` — TicketWonEvent contract (módulos downstream e novos caches)
- `ADR-005` — refresh token single-token strategy (JWT stateless, não cachear)
- `ConfigServiceImpl.java`, `AnalyticsServiceImpl.java`, `PermissionService.java` — arquivos alvo
