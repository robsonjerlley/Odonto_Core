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
- Java 21 + Spring Boot 3
- PostgreSQL (dois schemas: `identity_db` e `crm_db`)
- Spring Security + JWT (jjwt / io.jsonwebtoken)
- Spring Data JPA + Hibernate
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
  commercial/        → deals, negociação, configs do gestor  → crm_db
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
- **User**: id, name, email, passwordHash, sector(Sector), role(Role), active, createdBy, createdAt, updatedAt — implements UserDetails
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
- `AuthService` / `AuthServiceImpl` — login(), refreshToken()
- `UserDetailsServiceImpl` (@Component, implements UserDetailsService) — separado de UserService para evitar dependência circular
- `PermissionSeeder` (@Component, implements ApplicationRunner) — popula matriz padrão se vazia

### Security
- `UserPrincipal` (implements UserDetails) — objeto no SecurityContext, criado via `UserPrincipal.from(User)`
- `JwtService` — generateToken(), extractEmail(), isTokenValid() — claims: id, email, role, sector
- `JwtAuthFilter` (extends OncePerRequestFilter) — extrai Bearer token → valida → popula SecurityContext

---

## Módulo funnel

### Entidades (`crm_db`)
- **Customer**: id, name, phone, email?, cpf?(unique), source, adChannel?, adCampaign?, referredBy?(FK self), createdBy(UUID), createdAt, updatedAt
- **LeadTicket**: id, customerId, status, currentSector, assignedTo?, previousTicketId?(FK self), scheduledAt?, closedAt?, pendingAt?, recycledAt?, createdBy(UUID), createdAt, updatedAt
- **ContactLog**: id, ticketId, userId(UUID), channel, note(TEXT), statusBefore?, statusAfter?, occurredAt, createdAt — **imutável, sem UPDATE**

### Serviços
- `ICustomerService` / `CustomerServiceImpl` — create() abre LeadTicket automaticamente
- `ITicketService` / `TicketServiceImpl` — contém ALLOWED_TRANSITIONS, salva ContactLog automático em toda mudança de status
- `IContactLogService` / `ContactLogServiceImpl`

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
- **Deal**: id, ticketId, createdBySector(sempre AVALIACAO), createdBy(UUID), procedures(JSON → List\<DealProcedure\>), totalValue, discountPct=0, discountApprovedBy?, finalValue, paymentMethod?, closedBy?, closedAt?, archived=false, createdAt, updatedAt
- **DealProcedure** (record, JSON em Deal): name, code?, tableValue, quantity=1, note?
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
| Exceção a Interface+Impl | `RecycleJob`, `UserDetailsServiceImpl`, `PermissionSeeder` (implementam interfaces Spring) |
| Prefixo de repositórios | sem prefixo `I` — `UserRepository`, `DealRepository` etc |
| Mapeamento | MapStruct com `@Mapper(componentModel = "spring")` |
| Campos monetários | sempre `BigDecimal`, banco `NUMERIC(15,2)` |
| Enums no banco | sempre `@Enumerated(EnumType.STRING)` |
| Entidades imutáveis | `ContactLog`, `DealHistory` — sem `@Setter`, apenas INSERT |
| Cross-db | FKs para User em crm_db são `UUID` simples, sem `@ManyToOne` |
| Erros HTTP | 403 AccessDeniedException · 404 ResourceNotFoundException · 409 ResourceAlreadyExistsException · 422 IllegalStateException |

---

## Separação de bancos

```
identity_db  →  User, PermissionRule
crm_db       →  Customer, LeadTicket, ContactLog, Deal, DealHistory,
                AdsInvestment, RecycleConfig, BonusConfig
```
Dois DataSources configurados em `application.properties`. Cross-db via UUID — sem JOIN entre schemas no JPA.

---

## Ordem de implementação

1. `core/enums` — sem dependências
2. `identity` domain + repositories
3. `identity` security (UserPrincipal, JwtService, JwtAuthFilter, UserDetailsServiceImpl)
4. `identity` service (PermissionServiceImpl, PermissionSeeder)
5. `identity` service (UserServiceImpl, AuthServiceImpl) + SecurityConfig
6. `funnel` domain + repositories
7. `funnel` service (TicketServiceImpl primeiro, depois CustomerServiceImpl, ContactLogServiceImpl)
8. `commercial` domain + repositories
9. `commercial` service (ConfigServiceImpl, CommercialServiceImpl, RecycleJob)
10. `analytics` service + Result records
11. Mappers MapStruct + Controllers + DTOs request/response
