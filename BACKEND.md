# OdontoCore CRM — Documentação de Backend

CRM especializado para clínicas odontológicas com rastreabilidade financeira completa: do investimento em ADS até o fechamento do contrato, passando por cada setor que tocou o cliente.

---

## Stack

| Tecnologia | Uso |
|---|---|
| Java 21 + Spring Boot 3 | Runtime e framework principal |
| PostgreSQL | Dois schemas: `identity_db` e `crm_db` |
| Spring Security + JWT (jjwt) | Autenticação e autorização |
| Spring Data JPA + Hibernate | Persistência |
| MapStruct | Mapeamento Entity ↔ DTO |
| `@Scheduled` | Job noturno de reciclagem de leads |

---

## Estrutura de módulos

```
src/main/java/io/sertaoBit/odontocore/crm/
├── config/security/          → JWT filter, SecurityConfig, SecurityUtils
├── core/enums/               → enums globais compartilhados
├── exception/                → exceções de domínio
├── modules/
│   ├── identity/             → usuários, autenticação, permissões  → identity_db
│   ├── funnel/               → clientes, tickets, contatos         → crm_db
│   ├── commercial/           → deals, negociação, configs          → crm_db
│   └── analytics/            → métricas read-only cross-db
└── shared/                   → DTOs compartilhados (DataRangeDTO, DealProcedureDTO)
```

### Separação de bancos

```
identity_db  →  User, PermissionRule
crm_db       →  Customer, LeadTicket, ContactLog,
                Deal, DealHistory, AdsInvestment,
                RecycleConfig, BonusConfig
```

FKs para `User` dentro do `crm_db` são armazenadas como `UUID` simples — sem `@ManyToOne` entre schemas distintos. O JPA não faz JOIN entre bancos.

---

## Enums globais (`core/enums`)

| Enum | Valores |
|---|---|
| `Sector` | `LEADS`, `ATTENDANT`, `EVALUATOR`, `COMMERCIAL`, `ADM`, `MANAGER` |
| `Role` | `ADM_SYSTEM`, `ADM_LEADS`, `USER_LEADS`, `USER_ATTENDANT`, `ADM_EVALUATOR`, `USER_EVALUATOR`, `ADM_COMMERCIAL`, `USER_COMMERCIAL` |
| `TicketStatus` | `NEW`, `IN_CONTACT`, `SCHEDULED`, `IN_EVALUATION`, `NEGOTIATION`, `WIN`, `PENDING`, `RECYCLED`, `LOSS` |
| `CustomerSource` | `ADS_PAID`, `ORGANIC`, `INDICATION` |
| `AdsChannel` | `GOOGLE`, `META`, `INSTAGRAM`, `TIKTOK`, `OUTER` |
| `ContactChannel` | `ORGANIC`, `REFERRAL`, `FACEBOOK`, `INSTAGRAM`, `WHATSAPP`, `PHONE_CALL`, `WEBSITE_FROM`, `OTHER` |
| `Resource` | `CUSTOMER`, `USER`, `TICKET`, `CONTACT_LOG`, `DEAL`, `ANALYTICS`, `CONFIG` |
| `Action` | `CREATE`, `READ`, `UPDATE`, `CLOSE`, `RECYCLE`, `CONFIGURE`, `DELETE` |
| `PermissionScope` | `GLOBAL`, `SECTOR`, `OWN` |

---

## Fluxo completo do cliente

```
[1] LOGIN
    POST /api/v1/authentication/login
    → JWT retornado com claims: id, username, role, sector

[2] LEADS — Cadastro
    POST /api/v1/customers
    → Customer salvo em crm_db
    → LeadTicket(status=NEW, currentSector=user.sector) aberto automaticamente

[3] LEADS — Ciclo de contatos
    PATCH /api/v1/tickets/{id}/status
    → Valida ALLOWED_TRANSITIONS
    → Salva ContactLog automático a cada mudança
    → NEW → IN_CONTACT → SCHEDULED
    → (perda possível: IN_CONTACT → LOSS)

[4] EVALUATOR — Avaliação
    PATCH /api/v1/tickets/{id}/status  (IN_CONTACT → SCHEDULED → IN_EVALUATION)
    POST  /api/v1/deal/{ticketId}
    → Verifica permissão: DEAL + CREATE + EVALUATOR
    → Exige ticket.status == IN_EVALUATION
    → Calcula totalValue = Σ(tableValue × quantity)
    → Ticket avança: IN_EVALUATION → NEGOTIATION, currentSector = COMMERCIAL
    → (perda possível: IN_EVALUATION → LOSS)

[5] COMMERCIAL — Negociação
    PATCH /api/v1/deal/{id}/discount
    → finalValue = totalValue × (1 - discountPct/100), RoundingMode.HALF_UP
    → Registra DealHistory (imutável)

    PATCH /api/v1/deal/{id}/closeDeal
    → Verifica permissão: DEAL + CLOSE + COMMERCIAL
    → Deal: closedAt, closedBy, paymentMethod preenchidos
    → Ticket: status = WIN, closedAt = now
    → Registra DealHistory

[6] RECICLAGEM (caminho alternativo ao WIN)
    NEGOTIATION → PENDING (comercial perdeu contato)
    RecycleJob @ 02:00 diário:
    → Lê tickets PENDING cujo pendingAt expirou (>= afterDays da RecycleConfig)
    → Arquiva Deal ativo (archived = true)
    → Ticket original: status = RECYCLED
    → Cria novo LeadTicket(status=NEW, previousTicketId=original.id)
    → Cliente volta ao início da esteira com histórico preservado
```

### Transições permitidas (`ALLOWED_TRANSITIONS`)

```
NEW         → IN_CONTACT
IN_CONTACT  → SCHEDULED | LOSS
SCHEDULED   → IN_EVALUATION
IN_EVALUATION → NEGOTIATION | LOSS
NEGOTIATION → WIN | PENDING
PENDING     → RECYCLED          ← RecycleJob
RECYCLED    → NEW               ← novo LeadTicket filho
```

Transição inválida → `IllegalStateException` → HTTP 422.

---

## Módulo identity

### Entidades (`identity_db`)

**User**
```
id (UUID), name, username (unique), passwordHash,
sector (Sector), role (Role), active,
createdBy (UUID), createdAt, updatedAt
```

**PermissionRule**
```
id, role, sector (nullable), resource, action,
scope (GLOBAL | SECTOR | OWN), allowed, conditions (JSON)
```

### Autenticação

**`JwtAuthenticationFilter`** (executa em toda requisição protegida):
1. Extrai `Bearer <token>` do header `Authorization`
2. `JwtUtil` valida assinatura e expiração
3. `MainUserDetailsService` carrega o usuário pelo username
4. Popula `SecurityContext` com `MainUser`
5. Token ausente ou inválido → HTTP 401

**`SecurityUtils`** — utilitário injetável nos serviços:
- `getCurrentUser()` → `User` completo (com `sector`, `role`, `id`)
- `getCurrentUserId()` → `UUID` (chama `getCurrentUser()` internamente)

### Permissões (RBAC com escopo)

**`PermissionService`**:
```java
canAccess(user, resource, action, targetSector, targetOwnerId)
checkOrThrow(...)  // lança AccessDeniedException (HTTP 403) se negado
```

Resolução em duas etapas:
1. Busca regra por `(role + sector + resource + action)`
2. Fallback: busca por `(role + resource + action)`

Aplicação do escopo:
- `GLOBAL` → sempre permitido
- `SECTOR` → `user.sector == targetSector`
- `OWN` → `user.id == targetOwnerId`

**`PermissionSeeder`** — popula a matriz padrão de permissões na inicialização se a tabela estiver vazia.

### API — Users

| Método | Endpoint | Ação |
|---|---|---|
| `POST` | `/api/v1/users/create` | Cria usuário (por ADM) |
| `PATCH` | `/api/v1/users/updatePassword/{username}/passwordHash` | Atualiza senha |
| `GET` | `/api/v1/users/{id}` | Busca por ID (UUID) |
| `GET` | `/api/v1/users/username/{username}` | Busca por username (identificador único) |
| `GET` | `/api/v1/users?sector=&role=` | Filtra por setor e/ou role (ambos opcionais) |
| `DELETE` | `/api/v1/users/{id}` | Remove usuário |

### API — Auth

| Método | Endpoint | Ação |
|---|---|---|
| `POST` | `/api/v1/authentication/login` | Login → retorna JWT |

---

## Módulo funnel

### Entidades (`crm_db`)

**Customer**
```
id (UUID), name, phone, email?, cpf? (unique),
source (CustomerSource), adChannel? (AdsChannel), adCampaign?,
referredBy? (UUID self-ref), createdBy (UUID), createdAt, updatedAt
```

**LeadTicket**
```
id (UUID), customerId (UUID), status (TicketStatus),
currentSector (Sector), assignedTo? (UUID),
previousTicketId? (UUID self-ref), scheduledAt?,
closedAt?, pendingAt?, recycledAt?,
createdBy (UUID), createdAt, updatedAt
```

**ContactLog** *(imutável — apenas INSERT, sem UPDATE)*
```
id (UUID), ticketId (UUID), userId (UUID),
channel (ContactChannel), note (TEXT),
statusBefore? (TicketStatus), statusAfter? (TicketStatus),
occurredAt, createdAt
```

### Regras de negócio

- `CustomerService.create()` abre `LeadTicket(status=NEW)` automaticamente com `currentSector = user.sector` do usuário logado.
- `LeadTicketService.changeStatus()` salva um `ContactLog` automático a cada transição, independente de log manual.
- `ContactLog` é imutável: sem `@Setter` na entidade, sem endpoint de update.

### API — Customers

| Método | Endpoint | Ação |
|---|---|---|
| `POST` | `/api/v1/customers` | Cria customer + abre LeadTicket automaticamente |
| `PATCH` | `/api/v1/customers/{id}` | Atualiza dados do customer |
| `GET` | `/api/v1/customers/{id}` | Busca por ID (UUID) |
| `GET` | `/api/v1/customers/cpf/{cpf}` | Busca por CPF (identificador único — ver ADR-001) |
| `GET` | `/api/v1/customers?name=&phone=&adChannel=` | Filtros de busca (todos opcionais) |
| `DELETE` | `/api/v1/customers/{id}` | Remove customer |

### API — Tickets

| Método | Endpoint | Ação |
|---|---|---|
| `POST` | `/api/v1/tickets` | Cria ticket manualmente |
| `PATCH` | `/api/v1/tickets/{id}/status` | Muda status (valida ALLOWED_TRANSITIONS) |
| `GET` | `/api/v1/tickets/{id}` | Busca por ID (UUID) |
| `GET` | `/api/v1/tickets?customerId=&status=&assignedTo=` | Filtros de busca (todos opcionais) |
| `DELETE` | `/api/v1/tickets/{id}` | Remove ticket |

### API — Contact Logs

| Método | Endpoint | Ação |
|---|---|---|
| `POST` | `/api/v1/contact-logs` | Registra log de contato manual |
| `GET` | `/api/v1/contact-logs/{id}` | Busca por ID (UUID) |
| `GET` | `/api/v1/contact-logs?ticketId=` | Filtra logs pelo ticket |
| `DELETE` | `/api/v1/contact-logs/{id}` | Remove log |

---

## Módulo commercial

### Entidades (`crm_db`)

**Deal**
```
id (UUID), ticketId (UUID), createdBySector (sempre EVALUATOR),
createdBy (UUID), procedures (JSON → List<DealProcedure>),
totalValue (BigDecimal), discountPct = 0,
discountApprovedBy? (UUID), finalValue (BigDecimal),
paymentMethod?, closedBy? (UUID), closedAt?,
archived = false, createdAt, updatedAt
```

**DealProcedure** *(record serializado em JSON dentro de Deal)*
```
name, code?, tableValue (BigDecimal), quantity = 1, note?
```

**DealHistory** *(imutável — apenas INSERT)*
```
id (UUID), dealId (UUID), changedBy (UUID),
changedBySector (Sector), fieldChanged,
valueBefore (JSON), valueAfter (JSON), occurredAt
```

**AdsInvestment**
```
id (UUID), channel (AdsChannel), campaign?,
amount (BigDecimal), periodStart, periodEnd,
registeredBy (UUID), createdAt
```

**RecycleConfig**
```
id (UUID), sector? (null = global), afterDays,
active = true, configuredBy (UUID), createdAt, updatedAt
```

**BonusConfig**
```
id (UUID), sector, role, metricKey, bonusPct,
targetValue?, active = true, configuredBy (UUID),
periodRef, createdAt
```

### Regras de negócio críticas

- `totalValue = Σ (procedure.tableValue × procedure.quantity)` calculado em `BigDecimal`.
- `finalValue = totalValue × (1 - discountPct / 100)` com `RoundingMode.HALF_UP`.
- Todo valor monetário usa `BigDecimal` — nunca `double` ou `float`.
- Deal arquivado (`archived = true`) não aceita nenhuma alteração.
- Deal já fechado (`closedAt != null`) não pode ser fechado novamente.
- `DealHistory` é registrado a cada `update`, `applyDiscount` e `closeDeal`.

**RecycleJob** (`@Scheduled(cron = "0 0 2 * * *")`)
```
Para cada ticket PENDING:
  1. Busca RecycleConfig por setor → fallback para config global (sector = null)
  2. Se dias desde pendingAt >= afterDays:
     - Deal ativo → archived = true
     - ticket.status = RECYCLED
     - Cria novo LeadTicket(status=NEW, previousTicketId=ticket.id)
```

### API — Deal

| Método | Endpoint | Ação | Permissão exigida |
|---|---|---|---|
| `POST` | `/api/v1/deal/{ticketId}` | Cria deal | `DEAL + CREATE + EVALUATOR` |
| `PATCH` | `/api/v1/deal/{id}` | Atualiza procedimentos | `DEAL + UPDATE + EVALUATOR` ou `COMMERCIAL` |
| `PATCH` | `/api/v1/deal/{id}/discount` | Aplica desconto | `DEAL + UPDATE + COMMERCIAL` |
| `PATCH` | `/api/v1/deal/{id}/closeDeal` | Fecha deal | `DEAL + CLOSE + COMMERCIAL` |
| `GET` | `/api/v1/deal/{id}/dealHistory` | Deal + histórico completo | — |

### API — Config (gestor)

| Método | Endpoint | Ação |
|---|---|---|
| `POST` | `/api/v1/config/recycle` | Define prazo de reciclagem por setor |
| `POST` | `/api/v1/config/bonus` | Define configuração de bônus |
| `POST` | `/api/v1/config/ads-investment` | Registra investimento em ADS |

---

## Módulo analytics

Apenas leitura (`@Transactional(readOnly = true)`). Nunca escreve. Lê cross-db via repositórios injetados em memória — sem JOIN JPA entre schemas.

Todos os endpoints recebem `period` como `@ModelAttribute DataRangeDTO(from, to)` e passam `currentUserId` para verificação de permissão interna.

### API — Analytics

| Método | Endpoint | Query params | Retorno |
|---|---|---|---|
| `GET` | `/api/v1/analytics/ads-roi` | `channel`, `period` | `AdsRoiResultDTO` |
| `GET` | `/api/v1/analytics/conversion` | `period`, `sector` | `StageConversionResultDTO` |
| `GET` | `/api/v1/analytics/dropoff` | `period` | `List<SectorDropOffResultDTO>` |
| `GET` | `/api/v1/analytics/user-performance/{targetUserId}` | `period` | `UserPerformanceResultDTO` |
| `GET` | `/api/v1/analytics/bonus/{targetId}` | `periodRef` | `BigDecimal` |
| `GET` | `/api/v1/analytics/dashboard` | `period` | `GlobalDashBoardResultDTO` (ADM/MANAGER) |

### Métricas calculadas

- **ROI de ADS**: `Σ finalValue` dos deals WIN com `customer.adChannel = channel` ÷ `Σ AdsInvestment.amount` no período.
- **Conversão por estágio**: captados → agendados → deal criado → WIN, com percentuais.
- **Dropoff por setor**: onde a esteira perde mais clientes no período.
- **Performance individual**: por setor/role do usuário alvo — ATTENDANT: agendamentos; EVALUATOR: taxa de aceite; COMMERCIAL: fechamentos + desconto médio.
- **Bônus apurado**: `metricValue × BonusConfig.bonusPct`.

---

## Padrões do projeto

| Decisão | Padrão |
|---|---|
| Interfaces de serviço | `UserService` + `UserServiceImpl` — sem prefixo `I` |
| Exceção a Interface+Impl | `RecycleJob`, `MainUserDetailsService`, `PermissionSeeder` (implementam interfaces Spring) |
| Repositórios | sem prefixo — `UserRepository`, `DealRepository` |
| Mapeamento | MapStruct com `@Mapper(componentModel = "spring")` |
| Campos monetários | sempre `BigDecimal`, banco `NUMERIC(15,2)` |
| Enums no banco | sempre `@Enumerated(EnumType.STRING)` |
| Entidades imutáveis | `ContactLog`, `DealHistory` — sem `@Setter`, apenas INSERT |
| Cross-db | FKs para `User` em `crm_db` são `UUID` simples, sem `@ManyToOne` |
| Busca por identificador único | `GET /resource/{uniqueKey}` — retorna objeto único ou 404 (ver ADR-001) |
| Busca por filtros | `GET /resource?param=value` — retorna `List<DTO>` sempre, pode ser vazia (ver ADR-001) |
| Nomes de rotas | sem prefixos semânticos (`findBy`, `search`, `get`) em URLs (ver ADR-001) |

### Códigos de erro HTTP

| Código | Situação |
|---|---|
| 401 | Token ausente, inválido ou expirado |
| 403 | `AccessDeniedException` — permissão negada pelo RBAC |
| 404 | `ResourceNotFoundException` — entidade não encontrada |
| 409 | `ResourceAlreadyExistsException` — conflito (ex.: CPF duplicado) |
| 422 | `IllegalStateException` — transição de status inválida ou regra de negócio violada |
