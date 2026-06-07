# ADR-012: Padrão RBAC para operações de listagem e filtro scope-aware

**Status**: Aceito  
**Data**: 2026-06-05  
**Autores**: Arquiteto-Agent  
**Impacto**: `PermissionService.java`, `CustomerServiceImpl`, `LeadTicketServiceImpl`, `ContactLogServiceImpl`, repositories do módulo funnel

---

## Contexto

A ADR-004 estabeleceu três padrões de chamada ao `checkOrThrow()`. O Padrão 3 (SEARCH) previa:

> "Para listagens, não há um recurso específico para extrair sector/owner. Passa-se os dados do próprio usuário. Scope enforcement a nível de query é trabalho futuro (Fase 3)."

A Fase 3 foi iniciada na sessão de 2026-06-05. Durante a implementação, foram identificados dois problemas estruturais:

1. `checkOrThrow()` em `search()` faz um **auto-check** — passa `user.getSector()` e `user.getId()` como alvo, tornando o scope resolution trivialmente verdadeiro para o próprio usuário. Verifica apenas se a permissão existe, sem enforçar o scope real.

2. Se `search()` chama `checkOrThrow()` e depois `getScope()` separadamente para montar o filtro SQL, isso gera **2 queries à tabela `permission_rules`** por request — o `getScope()` interno do `checkOrThrow()` mais o `getScope()` explícito para o filtro.

Adicionalmente, foi avaliado o uso de **JPA Specifications** para filtros dinâmicos. A avaliação concluiu que é over-engineering dado o contexto: a matriz RBAC é estável, os perfis são definitivos para estes módulos, e os filtros de busca em `search()` são mutuamente exclusivos (apenas um entra por request).

---

## Decisão

### Padrão consolidado: dois contratos distintos para RBAC

**Padrão A — Single-resource** (CREATE, UPDATE, READ por ID, DELETE)

Sem mudança em relação à ADR-004. `checkOrThrow()` recebe os dados do recurso como alvo:

```java
permissionService.checkOrThrow(user, resource, action,
    entity.getCurrentSector(), entity.getCreatedBy());
```

**Padrão B — List operations** (SEARCH / listagens paginadas)

Substitui o Padrão 3 da ADR-004. `getScope()` serve como check E retorna o escopo para o filtro — uma única query à tabela `permission_rules`:

```java


`Optional.empty()` de `getScope()` significa ausência de permissão — o `orElseThrow()` faz o trabalho do 403 sem chamada extra.

---

### Rejeição de JPA Specifications

Descartado. A matriz RBAC é estável e os filtros de `search()` são mutuamente exclusivos. Specification adicionaria 3 novas classes, boilerplate sem retorno e abstração para um problema que não existe neste contexto.

**Regra derivada:** usar Specification apenas se filtros forem compostos E cumulativos E a matriz de permissões crescer. Nenhuma dessas condições se aplica a este projeto.

---

### Mapeamento de filtros por entidade

| Escopo | LeadTicket | Customer | ContactLog |
|--------|-----------|---------|-----------|
| GLOBAL | `findAll()` | `findAll()` | `findAll()` |
| OWN | `findByCreatedBy(userId)` | `findByCreatedBy(userId)` | `findByUserId(userId)` |
| SECTOR | `findByCurrentSector(sector)` | — sem campo — | — sem campo — |
| INTAKE | `findByCurrentSectorIn(sectors)` | — sem campo — | — sem campo — |

**Customer e ContactLog não possuem `currentSector`** — o setor é propriedade do `LeadTicket`, não do cliente nem do log. Forçar SECTOR/INTAKE nessas entidades exigiria JOINs desnecessários.

Decisão: implementar apenas `GLOBAL` e `OWN` para Customer e ContactLog. SECTOR e INTAKE em `search()` dessas entidades caem em `findAll()` — comportamento atual, sem regressão.

**Escopo INTAKE_SECTORS** (constante no service):
```java
private static final List<Sector> INTAKE_SECTORS = List.of(Sector.LEADS, Sector.ATTENDANT);
```

---

### Novos métodos de repository necessários

**LeadTicketRepository:**
```java
Page<LeadTicket> findByCreatedBy(UUID createdBy, Pageable pageable);
Page<LeadTicket> findByCurrentSector(Sector sector, Pageable pageable);
Page<LeadTicket> findByCurrentSectorIn(List<Sector> sectors, Pageable pageable);
```

**CustomerRepository:**
```java
Page<Customer> findByCreatedBy(UUID createdBy, Pageable pageable);
```

**ContactLogRepository:**
```java
Page<ContactLog> findByUserId(UUID userId, Pageable pageable);
```

---

### Módulos futuros: Financeiro e Agendamento

Os próximos módulos são **independentes do CRM** e recebem dados por transição de estado:

- **Financeiro**: recebe dados após `LeadTicket → WIN`
- **Agendamento**: recebe dados após Financeiro processar e `LeadTicket → IN_EVALUATION`

Padrão adotado: **Spring `ApplicationEventPublisher`** — barramento interno do Spring sem infraestrutura externa.

```java
// LeadTicketServiceImpl.changeStatus() — ao atingir WIN:
eventPublisher.publishEvent(new TicketWonEvent(ticket));

// FinancialService — listener:
@EventListener
public void onTicketWon(TicketWonEvent event) { ... }
```

Justificativa: módulos rodam no mesmo processo (monolito modular). Kafka/RabbitMQ seria over-engineering até que haja necessidade de extração para microsserviços. Se essa necessidade surgir, `ApplicationEventPublisher` é substituível por um broker sem mudança nos publishers/listeners além da anotação.

---

## Consequências Positivas

- Uma única query por `search()` para verificação de permissão e seleção do filtro
- `search()` com scope enforcement real no SQL — Fase 3 resolvida para LeadTicket
- Padrão documentado e extensível para novos services
- Módulos financeiro e agendamento desacoplados desde o início

## Consequências Negativas / Riscos

- Customer e ContactLog ficam com SECTOR/INTAKE sem filtro SQL real — retornam `findAll()` para esses scopes. Aceitável: setor é propriedade do ticket, não do cliente ou do log.
- `ApplicationEventPublisher` é síncrono por padrão — se o listener do Financeiro lançar exceção, a transação do `changeStatus()` faz rollback. Usar `@Async` no listener se processamento lento for esperado.

## Alternativas Consideradas

- **JPA Specifications**: descartado — over-engineering para contexto com matriz estável e filtros exclusivos.
- **`@Query` com parâmetros opcionais**: descartado — fragil com enums e Pageable, difícil de manter.
- **Kafka/RabbitMQ para módulos futuros**: descartado para MVP — infraestrutura desnecessária enquanto os módulos rodam no mesmo processo.

---

## Referências Cruzadas

- `ADR-004` — Padrão 3 (SEARCH) substituído por este ADR para listagens
- `ADR-011` — escopo INTAKE e `PermissionService.getScope()`
- `PermissionService.java` — `getScope()` público retornando `Optional<PermissionScope>`
- `security-gaps-funnel-permission.md` — Fase 3 definida como débito técnico