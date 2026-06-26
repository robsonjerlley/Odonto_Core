# OdontoCore CRM — Contexto do Projeto

## O que é
CRM especializado para clínicas odontológicas. O diferencial é a rastreabilidade financeira completa: do real investido em ADS até o real faturado no fechamento, passando por cada setor que tocou o cliente. Gestores visualizam gargalos, ROI de mídia paga e base para remuneração variável de funcionários.

---

## Contexto de negócio — o diferencial real

A maioria dos CRMs do mercado controla contatos, mas **não fecham o laço financeiro por setor**. O OdontoCore resolve isso.

### Origem dos clientes

| Origem | Como chega | `CustomerSource` |
|---|---|---|
| ADS pago | Meta, Google, TikTok — cliente viu anúncio e entrou em contato | `ADS_PAID` |
| Orgânico | Viu a clínica, ligou direto, entrou presencialmente | `ORGANIC` |
| Indicação | Outro paciente indicou | `INDICATION` |

O `adChannel` e `adCampaign` em `Customer` permitem ao gestor calcular o ROI exato por campanha: quanto foi investido naquele canal vs. quanto faturou em fechamentos de clientes vindos dele.

### A esteira por setor

```
LEADS (Sector.LEADS)
  ↓  cadastra Customer + abre LeadTicket(NEW)
  ↓  faz contatos → ContactLog por interação
  ↓  converte → SCHEDULED → currentSector = EVALUATOR
  ↓  não converte → LOSS
     └─ gargalo mensurável: ADS caro + baixa conversão em agendamentos

EVALUATOR (Sector.EVALUATOR)
  ↓  lê histórico de contatos, avalia o paciente
  ↓  cria Deal com procedimentos e valores estimados
  ↓  Deal criado → NEGOTIATION → currentSector = COMMERCIAL
  ↓  cliente recusa tratamento → LOSS
     └─ gargalo mensurável: taxa de aceite do plano de tratamento

COMMERCIAL (Sector.COMMERCIAL)
  ↓  lê histórico + Deal, negocia valores e descontos
  ↓  fecha → WIN → closedAt + closedBy registrados
  ↓  não fecha → PENDING (comercial perdeu contato)
     └─ após X dias configurados pelo gestor → RecycleJob → RECYCLED
        └─ novo LeadTicket(NEW) com previousTicketId apontando para o anterior
           └─ cliente volta ao LEADS para novo ciclo de contato
```

### Por que o reciclo importa
O cliente pode ter sido perdido por má abordagem comercial, timing errado ou mudança de situação. O reciclo não é falha — é uma segunda chance estruturada. O gestor configura o prazo de pendência por setor (`RecycleConfig.afterDays`), evitando que leads fiquem esquecidos indefinidamente.

### O que as métricas fecham

- **ROI de ADS**: investimento por canal (`AdsInvestment`) ÷ receita dos `Deal` fechados de clientes com aquele `adChannel`
- **Gargalo de setor**: entradas vs. conversões por setor no período — onde a esteira perde mais
- **Performance individual**: cada funcionário tem métricas próprias por papel (ATTENDANT: agendamentos / DENTIST: aceite de tratamento / SELLER: fechamentos e desconto médio)
- **Remuneração variável**: `BonusConfig` define meta e percentual por setor+role — `getBonusApurado()` calcula automaticamente

### Regra de ouro
Nenhuma FK cross-db com `@ManyToOne`. Clientes, tickets e deals referenciam `User` apenas pelo `UUID` (`createdBy`, `assignedTo`, `closedBy`). O JPA não faz JOIN entre `identity_db` e `crm_db` — Analytics lê os dois via repositórios injetados em memória.

---

## Stack
- Java 21 + Spring Boot 4
- PostgreSQL (dois schemas: `identity_db` e `crm_db`)
- Spring Security + JWT (jjwt / io.jsonwebtoken)
- Spring Data JPA + Hibernate 7 (`@TenantId` discriminator multi-tenant — ADR-024)
- Flyway (versionamento de schema — migrations em `src/main/resources/db/migration/`)
- MapStruct (mapeamento Entity ↔ DTO)
- `@Scheduled` para job noturno de reciclo

---

## Package raiz
`io.sertaoBit.odontocore.crm`

---

## Estrutura de módulos

```
core/enums/          → enums globais compartilhados por toda a aplicação
modules/
  identity/          → autenticação, usuários, permissões  → identity_db
  funnel/            → clientes, tickets, histórico de contato  → crm_db
  catalog/           → catálogo de procedimentos por clínica  → crm_db  (ADR-026)
  commercial/        → deals, negociação, configs do gestor  → crm_db  (depende de catalog)
  analytics/         → métricas read-only cross-db  → lê os dois bancos
```

---

## Enums globais (`core/enums`)

| Enum | Valores |
|---|---|
| `Sector` | LEADS, ATTENDANT, EVALUATOR, COMMERCIAL, ADM, MANAGER |
| `Role` | ADM_SYSTEM, ADM_LEADS, USER_LEADS, USER_ATTENDANT, ADM_EVALUATOR, USER_EVALUATOR, ADM_COMMERCIAL, USER_COMMERCIAL |
| `TicketStatus` | NEW, IN_CONTACT, SCHEDULED, IN_EVALUATION, NEGOTIATION, WIN, PENDING, RECYCLED, LOSS |
| `CustomerSource` | ADS_PAID, ORGANIC, INDICATION |
| `AdsChannel` | GOOGLE, META, INSTAGRAM, TIKTOK, OUTER |
| `ContactChannel` | ORGANIC, REFERRAL, FACEBOOK, INSTAGRAM, WHATSAPP, PHONE_CALL, WEBSITE_FROM, OTHER |
| `PaymentMethod` | PIX(1.00), CASH(1.00), DEBIT_CARD(0.98), CREDIT_CARD(0.97), INSTALLMENT(0.85), DENTAL_INSURANCE(0.90) — cada valor carrega `conversionFactor` (BigDecimal) para cálculo de `expectedCash` em analytics |
| `Resource` | CUSTOMER, USER, TICKET, CONTACT_LOG, DEAL, ANALYTICS, CONFIG |
| `Action` | CREATE, READ, UPDATE, CLOSE, RECYCLE, CONFIGURE, DELETE |
| `PermissionScope` | GLOBAL, SECTOR, OWN |

Regra: sempre `@Enumerated(EnumType.STRING)` nas entidades JPA.

---

## Fluxo do cliente (esteira)

```
[Entrada] ADS_PAID | ORGANIC | INDICATION
    ↓
[LEADS] cadastra Customer + abre LeadTicket(NEW)
        faz contatos → registra ContactLog
        converte → status SCHEDULED → currentSector = EVALUATOR
    ↓ (perda possível: não agenda → LOSS)
[EVALUATOR] lê histórico, avalia paciente
            cria Deal com procedimentos e valores
            Deal criado → status NEGOTIATION → currentSector = COMMERCIAL
    ↓ (perda possível: recusa tratamento → LOSS)
[COMMERCIAL] lê histórico + Deal
             negocia valores e descontos
             fecha → WIN
             ou não fecha → PENDING → RecycleJob após X dias → novo ciclo
```

---

## Módulo identity

### Entidades (`identity_db`)
- **User**: id, clinicId(ADR-022), name, username, password, sector(Sector), role(Role), active, createdBy, createdAt, updatedAt — entidade JPA pura (o `UserDetails` é o `MainUser`, criado via `MainUser.form(User)`)
- **PermissionRule**: id, role, sector(nullable), resource, action, scope, allowed, conditions(JSON)

### Padrão de permissões (RBAC com escopo)
```java
// chamada padrão antes de qualquer operação crítica
permissionService.checkOrThrow(currentUser, Resource.DEAL, Action.CREATE, ticket.getCurrentSector(), null);
// lança AccessDeniedException (HTTP 403) se negado
```
Resolução: busca regra (role+sector+resource+action) → fallback (role+resource+action) → aplica scope: GLOBAL=sempre | SECTOR=user.sector==targetSector | OWN=user.id==targetOwnerId

### Serviços
- `PermissionService` / `PermissionServiceImpl` — canAccess(), checkOrThrow()
- `UserService` / `UserServiceImpl` — createUser(), deactivateUser(), listUsersBySector()
- `AuthService` (@Service, **sem interface**) — login(username, password), refreshToken(dto)
- `MainUserDetailsService` (@Service, implements UserDetailsService) — loadUserByUsername(); separado de UserService para evitar dependência circular
- `PermissionSeeder` (@Component, implements ApplicationRunner) — popula matriz padrão se vazia

### Security
- `MainUser` (implements UserDetails) — objeto no SecurityContext, criado via `MainUser.form(User)`; carrega `clinicId` (ADR-022)
- `JwtUtil` — generateToken(), extractUsername(), extractClinicId(), isValid() — claims: id, role, sector, clinicId (subject = username)
- `JwtAuthenticationFilter` (extends OncePerRequestFilter) — extrai Bearer token → valida → popula SecurityContext → popula `TenantContext` via `jwtUtil.extractClinicId(token)` + `try/finally clear()` (ADR-024)
- `SecurityUtils` — getCurrentUser() / getCurrentUserId() a partir do SecurityContext

---

## Módulo funnel

### Entidades (`crm_db`)
- **Customer**: id, `@TenantId` clinicId, name, phone, email?, cpf?(unique), source, adChannel?, adCampaign?, referredBy?(FK self), createdBy(UUID), createdAt, updatedAt
- **LeadTicket**: id, `@TenantId` clinicId, customerId, status, currentSector, assignedTo?, previousTicketId?(FK self), scheduledAt?, closedAt?, pendingAt?, recycledAt?, createdBy(UUID), createdAt, updatedAt
- **ContactLog**: id, `@TenantId` clinicId, ticketId, userId(UUID), channel, note(TEXT), statusBefore?, statusAfter?, occurredAt, createdAt — **imutável, sem UPDATE**

### Serviços
- `CustomerService` / `CustomerServiceImpl` — create() abre LeadTicket automaticamente
- `LeadTicketService` / `LeadTicketServiceImpl` — contém ALLOWED_TRANSITIONS, salva ContactLog automático em toda mudança de status
- `ContactLogService` / `ContactLogServiceImpl`

### ALLOWED_TRANSITIONS
```
NEW → {IN_CONTACT}
IN_CONTACT → {SCHEDULED, LOSS}
SCHEDULED → {IN_EVALUATION}          ← automático via schedule()
IN_EVALUATION → {NEGOTIATION, LOSS}
NEGOTIATION → {WIN, PENDING}
PENDING → {RECYCLED}                  ← RecycleJob
RECYCLED → {NEW}                      ← novo LeadTicket filho
```
Transição inválida → `IllegalStateException` → HTTP 422

---

## Módulo commercial

### Entidades (`crm_db`)
- **Deal**: id, ticketId, createdBySector(sempre AVALIACAO), createdBy(UUID), procedures(JSON → List\<DealProcedure\>), totalValue, discountPct=0, discountApprovedBy?, finalValue, paymentMethod?(PaymentMethod enum), closedBy?, closedAt?, archived=false, createdAt, updatedAt
- **DealProcedure** (record, JSON em Deal — snapshot): procedureId(FK lógica→Procedure), name(snapshot), code?(snapshot), tableValue(snapshot), priceOverride?(negociado neste deal), quantity=1, note? — ver ADR-026
- **DealHistory**: id, dealId, changedBy(UUID), changedBySector, fieldChanged, valueBefore(JSON), valueAfter(JSON), occurredAt — **imutável**
- **AdsInvestment**: id, channel, campaign?, amount, periodStart, periodEnd, registeredBy(UUID), createdAt
- **RecycleConfig**: id, sector?(null=global), afterDays, active=true, configuredBy(UUID), createdAt, updatedAt
- **BonusConfig**: id, sector, role, metricKey, bonusPct, targetValue?, active=true, configuredBy(UUID), periodRef, createdAt

### Serviços
- `CommercialService` / `CommercialServiceImpl` — createDeal(), updateDeal(), applyDiscount(), closeDeal()
- `ConfigService` / `ConfigServiceImpl` — setRecycleConfig(), setBonusConfig(), registerAdsInvestment()
- `RecycleJob` (@Component, @Scheduled) — **não segue Interface+Impl**, roda 02:00 diário

### Regras de negócio críticas
- `finalValue = totalValue × (1 - discountPct/100)` com `RoundingMode.HALF_UP`
- Todo campo monetário usa `BigDecimal` — nunca `double` ou `float`
- Desconto acima de `conditions.maxDiscountPct` da PermissionRule → lança `DiscountApprovalRequiredException`
- RecycleJob: `ChronoUnit.DAYS.between(pendingAt, now) >= config.afterDays` → arquiva Deal atual → marca ticket RECICLADO → cria novo LeadTicket(previousTicketId=original.id)

---

## Módulo analytics

### Serviço
- `AnalyticsService` / `AnalyticsServiceImpl` — `@Transactional(readOnly=true)` — **nunca escreve**
- Lê cross-db via repositórios injetados — sem @ManyToOne entre bancos distintos

### Métodos principais
```
getAdsROI(channel, period) → receita(Deals GANHO com adChannel=channel) ÷ custo(AdsInvestment)
getConversionByStage(period, sector) → captados / agendados / dealCriado / ganho com %
getDropoffBySector(period) → onde a esteira perde mais clientes
getUserPerformance(targetUserId, period) → métricas por setor/role do usuário alvo (ATTENDANT: agendamentos; EVALUATOR: aceite de tratamento; COMMERCIAL: fechamentos+desconto médio)
getBonusApurado(userId, periodRef) → metricValue × BonusConfig.bonusPct
getGlobalDashboard(period) → apenas ADMIN/MANAGER — agrega tudo
```

### DTOs de retorno (sufixo *Result)
`AdsROIResult`, `StageConversionResult`, `SectorDropoffResult`, `UserPerformanceResult`, `GlobalDashboardResult`, `DateRange`(record)

---

## Padrões adotados no projeto

| Decisão | Padrão |
|---|---|
| Interfaces de serviço | `UserService` + `UserServiceImpl` em **todos** os serviços — sem prefixo `I` |
| Exceção a Interface+Impl | `RecycleJob`, `MainUserDetailsService`, `AuthService`, `PermissionSeeder` |
| Prefixo de repositórios | sem prefixo `I` — `UserRepository`, `DealRepository` etc |
| Mapeamento | MapStruct com `@Mapper(componentModel = "spring")` |
| Campos monetários | sempre `BigDecimal`, banco `NUMERIC(15,2)` |
| Enums no banco | sempre `@Enumerated(EnumType.STRING)` |
| Entidades imutáveis | `ContactLog`, `DealHistory` — sem `@Setter`, apenas INSERT |
| Cross-db | FKs para User em crm_db são `UUID` simples, sem `@ManyToOne` |
| Erros HTTP | 403 AccessDeniedException · 404 ResourceNotFoundException · 405 HttpRequestMethodNotSupportedException · 409 ResourceAlreadyExistsException · 422 IllegalStateException |
| Pageable em controllers | sempre `@ParameterObject @PageableDefault(size = 20) Pageable pageable` — `@ParameterObject` expande os params no Swagger UI evitando erro de sort com array vazio |
| Busca por identificador único | `GET /resource/{uniqueKey}` — retorna objeto único ou 404 (ver `.claude/adr/ADR-001`) |
| Busca por filtros | `GET /resource?param=value` — retorna `List<DTO>` sempre, pode ser vazia (ver `.claude/adr/ADR-001`) |
| Nomes de rotas | sem prefixos semânticos (`findBy`, `search`, `get`) em URLs — proibido (ver `.claude/adr/ADR-001`) |
| Interface de service | expõe apenas o que o consumidor externo chama — sub-rotinas internas ficam `private` na impl (ver `.claude/adr/ADR-002`) |

---

## Separação de bancos

```
identity_db  →  User, PermissionRule
crm_db       →  Customer, LeadTicket, ContactLog, Deal, DealHistory,
                AdsInvestment, RecycleConfig, BonusConfig, Procedure
```
Dois DataSources configurados em `application.properties`. Cross-db via UUID — sem JOIN entre schemas no JPA.

**Schema / migrations (greenfield, ADR-027):** a única migration é `db/migration/V1__create_schemas.sql` (só `CREATE SCHEMA IF NOT EXISTS`). As **tabelas/colunas vêm do Hibernate `ddl-auto=update`**, não do Flyway. Módulo novo **não** precisa de migration própria nesta fase. DDL de tabela no Flyway só entra na migração futura para `ddl-auto=validate`. ⚠️ Editar/renomear a V1 só é seguro antes de prod gravar o `flyway_schema_history` — depois disso, sempre V2+ (risco de checksum).

---

## Índice de ADRs (`.claude/adr/`)

| ADR | Título | Status |
|---|---|---|
| [001](../adr/ADR-001-api-search-lookup-pattern.md) | API search/lookup pattern (`{uniqueKey}` vs `?filtro`) | Aceito |
| [002](../adr/ADR-002-interface-vs-impl-encapsulamento.md) | Interface vs Impl — encapsulamento de service | Aceito |
| [003](../adr/ADR-003-contactlog-imutabilidade-delete-proibido.md) | ContactLog imutável — DELETE proibido | Aceito |
| [004](../adr/ADR-004-rbac-padrao-checkorThrow-funnel.md) | RBAC — padrão `checkOrThrow` no funnel | Aceito |
| [005](../adr/ADR-005-refresh-token-single-token-strategy.md) | Refresh token — single-token strategy (JWT) | Aceito |
| [006](../adr/ADR-006-customer-anonimizacao-lgpd-delete-ticket-removido.md) | Customer — anonimização LGPD, delete de ticket removido | Aceito |
| [007](../adr/ADR-007-config-get-endpoints-recycle-global-bonus-result-dto.md) | Config GET endpoints — recycle global + bonus Result DTO | Aceito |
| [008](../adr/ADR-008-payment-method-enum-conversion-factor.md) | PaymentMethod enum — conversionFactor | Aceito |
| [009](../adr/ADR-009-timezone-jvm-brasilia.md) | Timezone JVM — Brasília | Aceito |
| [011](../adr/ADR-011-intake-scope-cross-sector-acesso.md) | Intake scope — acesso cross-sector | Aceito |
| [012](../adr/ADR-012-rbac-fase3-padrao-list-vs-single-resource.md) | RBAC fase 3 — padrão list vs single resource | Aceito |
| [013](../adr/ADR-013-jpa-specifications-listagens-scope-aware.md) | JPA Specifications — listagens scope-aware | Aceito |
| [014](../adr/ADR-014-ferramenta-de-migracao-flyway.md) | Ferramenta de migração — Flyway | Aceito |
| [015](../adr/ADR-015-analytics-scope-aware-queries.md) | Analytics — scope-aware queries | Aceito |
| [016](../adr/ADR-016-bonus-mensal-vs-metricas-por-range-user-performance.md) | Bônus mensal vs métricas por range (user performance) | Aceito |
| [017](../adr/ADR-017-dashboard-global-range-livre-desacoplado-do-bonus.md) | Dashboard global — range livre, desacoplado do bônus | Aceito |
| [018](../adr/ADR-018-customer-anonimizado-excluido-da-listagem.md) | Customer anonimizado — excluído da listagem | Aceito |
| [019](../adr/ADR-019-contactlog-username-resolvido-no-backend.md) | ContactLog — username resolvido no backend | Aceito |
| [020](../adr/ADR-020-virtual-threads-tomcat-executor.md) | Virtual Threads — Tomcat executor | Aceito |
| [021](../adr/ADR-021-ads-investment-overlap-query.md) | AdsInvestment — overlap query (ROI) | Aceito |
| [022](../adr/ADR-022-multitenant-clinicid-foundation.md) | Multi-tenancy foundation — `clinicId` em User + JWT | Aceito (enforcement → ADR-024) |
| [023](../adr/ADR-023-ticket-won-event-contract.md) | TicketWonEvent — contrato do evento de fechamento | Aceito |
| [024](../adr/ADR-024-tenant-isolation-enforcement-tenantid.md) | Tenant isolation enforcement — `@TenantId` + `TenantContext` | Implementado |
| [025](../adr/ADR-025-rls-postgresql-defense-in-depth.md) | Row Level Security (PostgreSQL) — defesa em profundidade | Proposto (futuro) |
| [026](../adr/ADR-026-procedure-catalog-deal-snapshot.md) | Catálogo de Procedimentos (`Procedure`) + snapshot em `DealProcedure` | Aceito |
| [027](../adr/ADR-027-boot-fixes-schema-flyway-tenant-sentinel.md) | Correções de boot — schema, Flyway (PG18), sentinela de tenant, seed admin | Implementado |
| [028](../adr/ADR-028-catalog-read-boundary-provider-search.md) | Fronteira de leitura do `catalog` — `ProcedureProvider` (read-model `ProcedureView`) + `search()` unificado (revisa 026) | Aceito |

> **Multi-tenancy (trilha 022 → 024 → 025)**: 022 estabelece a fundação (`clinicId` em User/JWT, implementado); 024 implementado — `@TenantId` + `TenantContext` — isolamento automático no ORM ativo; 025 documenta RLS no PostgreSQL como defesa em profundidade futura. O Redis **não** é coberto por nenhuma — chave de cache com `clinicId` é sempre manual (ver `.claude/specs/spec-redis-cache.md`). **Boot greenfield (027)**: V1 reescrita para `CREATE SCHEMA` (Hibernate `ddl-auto=update` cria as tabelas); Flyway desabilitado só no local (PG18); `ClinicResolveTenant` usa sentinela `NO_TENANT` fora de request.

### Specs (`.claude/specs/`)

| Spec | Título | Status |
|---|---|---|
| [spec-redis-cache](../specs/spec-redis-cache.md) | Cache com Redis — chaves multi-tenant por `clinicId` | Backlog (pós ADR-022/024) |

---

## Ordem de implementação

1. `core/enums` — sem dependências
2. `identity` domain + repositories
3. `identity` security (MainUser, JwtUtil, JwtAuthenticationFilter, MainUserDetailsService)
4. `identity` service (PermissionServiceImpl, PermissionSeeder)
5. `identity` service (UserServiceImpl, AuthService) + SecurityConfig
6. `funnel` domain + repositories
7. `funnel` service (TicketServiceImpl primeiro, depois CustomerServiceImpl, ContactLogServiceImpl)
8. `commercial` domain + repositories
9. `commercial` service (ConfigServiceImpl, CommercialServiceImpl, RecycleJob)
10. `analytics` service + Result records
11. Mappers MapStruct + Controllers + DTOs request/response
