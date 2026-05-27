# US — Módulo Pós-Procedimento

**Épico:** Rastreabilidade completa do cliente — do contato inicial ao pós-procedimento
**Prioridade:** Must Have
**RICE Score:** 1.2 (Reach=1.0 · Impact=3 · Confidence=0.8 / Effort=2)
**Status:** Backlog — aguardando implementação

---

## Contexto e Motivação

O propósito fundacional do OdontoCore CRM é rastrear o cliente "desde o contato inicial até o pós procedimentos". Atualmente o sistema encerra o ciclo no status `WIN` (fechamento comercial), mas o WIN representa apenas o fechamento da venda — o procedimento odontológico ainda nem foi realizado. O relacionamento com o paciente continua após o WIN e o sistema precisa suportar esse ciclo.

### Fluxo atual (incompleto)
```
LEADS → EVALUATOR → COMMERCIAL → WIN  ← fim atual
```

### Fluxo esperado (completo)
```
LEADS → EVALUATOR → COMMERCIAL → WIN → POST_PROCEDURE
                                              ↓              ↓
                                         SCHEDULED       LOSS
                                       (retorno)     (não retornou)
```

---

## Impacto no Domínio

### 1. Enum `TicketStatus` — novo valor
```
POST_PROCEDURE  → ticket está no ciclo de acompanhamento pós-procedimento
```

### 2. `ALLOWED_TRANSITIONS` — novas transições
```
WIN             → {POST_PROCEDURE}
POST_PROCEDURE  → {SCHEDULED, LOSS}
```
- `POST_PROCEDURE → SCHEDULED`: paciente agenda retorno (entra novamente no ciclo de avaliação)
- `POST_PROCEDURE → LOSS`: paciente não retorna após tentativas de contato

### 3. `LeadTicket` — novo campo
```
procedurePerformedAt  : LocalDateTime  (nullable) — data em que o procedimento foi realizado
returnScheduledAt     : LocalDateTime  (nullable) — data agendada para retorno
```

### 4. `Customer` — novo campo
```
initialNote  : TEXT  (nullable) — observação livre no momento do cadastro
```
> Requisito adjacente identificado na análise de gaps. Baixo esforço, incluso nesta spec.

### 5. Analytics — nova métrica
```
getReturnRate(period)     → (tickets POST_PROCEDURE → SCHEDULED) / total POST_PROCEDURE no período
getPostProcedureLoss(period) → tickets POST_PROCEDURE → LOSS no período
```

---

## User Stories

---

### US-PPR-01 — Marcar procedimento como realizado

```
Como um USER_ATTENDANT, USER_LEADS ou ADM_SYSTEM,
Eu quero marcar um ticket WIN como pós-procedimento (POST_PROCEDURE),
Para que o sistema registre que o procedimento foi realizado e inicie o ciclo de acompanhamento.
```

> **Contexto de papéis:** USER_ATTENDANT é o atendente local (inbound — recebe o paciente) e
> USER_LEADS pode atuar no follow-up pós-procedimento (outbound — contacta o paciente para
> acompanhamento e retorno). Ambos têm responsabilidade legítima nesta transição.

#### Critérios de Aceite

```
Dado que existe um LeadTicket com status WIN,
E o usuário autenticado tem role USER_ATTENDANT, USER_LEADS, ADM_LEADS ou ADM_SYSTEM,
Quando o usuário faz PATCH /api/v1/tickets/{id}/status com body { "status": "POST_PROCEDURE" },
Então o ticket deve ter status atualizado para POST_PROCEDURE,
E o campo procedurePerformedAt deve ser preenchido com a data/hora atual,
E um ContactLog deve ser registrado automaticamente com note="Procedimento realizado. Início do acompanhamento pós-procedimento." e statusBefore=WIN, statusAfter=POST_PROCEDURE,
E o currentSector do ticket deve ser atualizado para ATTENDANT.
```

```
Dado que existe um LeadTicket com status diferente de WIN,
Quando o usuário tenta transicionar para POST_PROCEDURE,
Então o sistema deve retornar HTTP 422 com mensagem "Transição inválida: [status_atual] → POST_PROCEDURE".
```

```
Dado que o usuário autenticado tem role USER_EVALUATOR ou USER_COMMERCIAL,
Quando tenta marcar um ticket como POST_PROCEDURE,
Então o sistema deve retornar HTTP 403.
```

#### Implementação — Padrão TRANSITION_ROLES

A autorização por transição não deve ser gerenciada apenas pelo `PermissionSeeder` (que controla
`Resource + Action` de forma genérica). Transições específicas da máquina de estados requerem um
mapa dedicado no `LeadTicketServiceImpl`:

```java
private static final Map<TicketStatus, Set<Role>> TRANSITION_ROLES = Map.of(
    POST_PROCEDURE, Set.of(USER_ATTENDANT, USER_LEADS, ADM_LEADS, ADM_SYSTEM),
    WIN,            Set.of(USER_COMMERCIAL, ADM_COMMERCIAL, ADM_SYSTEM),
    LOSS,           Set.of(USER_LEADS, ADM_LEADS, ADM_SYSTEM)
    // transições sem entrada = qualquer role com TICKET/UPDATE pode executar
);
```

Em `changeStatus()`, após validar `ALLOWED_TRANSITIONS`, verificar `TRANSITION_ROLES`:

```java
Set<Role> allowedRoles = TRANSITION_ROLES.get(targetStatus);
if (allowedRoles != null) {
    Role currentRole = securityUtils.getCurrentUser().getRole();
    if (!allowedRoles.contains(currentRole)) {
        throw new AccessDeniedException("Role " + currentRole + " não pode executar esta transição.");
    }
}
```

---

### US-PPR-02 — Registrar contato de acompanhamento pós-procedimento

```
Como um USER_ATTENDANT,
Eu quero registrar contatos com o paciente após o procedimento,
Para que o histórico de acompanhamento pós-procedimento seja rastreável e auditável.
```

#### Critérios de Aceite

```
Dado que existe um LeadTicket com status POST_PROCEDURE,
Quando o usuário faz POST /api/v1/contact-logs com ticketId e channel e note preenchidos,
Então o ContactLog deve ser criado com os dados informados,
E o registro deve ser imutável (sem endpoint de UPDATE ou DELETE para ContactLog).
```

```
Dado que o campo note está em branco,
Quando o usuário tenta criar o ContactLog,
Então o sistema deve retornar HTTP 400 com mensagem "O campo 'note' é obrigatório para registro de contato.".
```

```
Dado que o LeadTicket não está em status POST_PROCEDURE,
Quando o usuário tenta registrar um ContactLog com esse ticketId,
Então o sistema deve aceitar normalmente — ContactLog não é restrito a status específico.
```

---

### US-PPR-03 — Agendar retorno do paciente

```
Como um USER_ATTENDANT,
Eu quero agendar o retorno de um paciente que está em pós-procedimento,
Para que o sistema registre a intenção de retorno e reinsira o paciente no ciclo de avaliação.
```

#### Critérios de Aceite

```
Dado que existe um LeadTicket com status POST_PROCEDURE,
Quando o usuário faz PATCH /api/v1/tickets/{id}/status com body { "status": "SCHEDULED", "returnScheduledAt": "2026-06-15T10:00:00" },
Então o ticket deve ter status atualizado para SCHEDULED,
E o campo returnScheduledAt deve ser persistido,
E o currentSector deve ser atualizado para EVALUATOR,
E um ContactLog automático deve ser registrado com note="Retorno agendado para [data]." e statusBefore=POST_PROCEDURE, statusAfter=SCHEDULED.
```

```
Dado que o campo returnScheduledAt não foi informado,
Quando o usuário tenta transicionar POST_PROCEDURE → SCHEDULED,
Então o sistema deve retornar HTTP 400 com mensagem "O campo 'returnScheduledAt' é obrigatório para agendamento de retorno.".
```

```
Dado que returnScheduledAt é uma data no passado,
Quando o usuário tenta fazer a transição,
Então o sistema deve retornar HTTP 400 com mensagem "A data de retorno não pode ser no passado.".
```

---

### US-PPR-04 — Registrar perda no pós-procedimento

```
Como um USER_ATTENDANT,
Eu quero marcar um ticket POST_PROCEDURE como LOSS,
Para que o sistema registre que o paciente não retornou após as tentativas de acompanhamento.
```

#### Critérios de Aceite

```
Dado que existe um LeadTicket com status POST_PROCEDURE,
Quando o usuário faz PATCH /api/v1/tickets/{id}/status com body { "status": "LOSS", "lossReason": "Paciente não retornou após 3 tentativas de contato." },
Então o ticket deve ter status atualizado para LOSS,
E um ContactLog automático deve ser registrado com note=[lossReason informado] e statusBefore=POST_PROCEDURE, statusAfter=LOSS,
E o ticket deve ser arquivado (archived=true no Deal vinculado, se existir).
```

```
Dado que o campo lossReason não foi informado,
Quando o usuário tenta transicionar POST_PROCEDURE → LOSS,
Então o sistema deve retornar HTTP 400 com mensagem "O motivo da perda é obrigatório.".
```

---

### US-PPR-05 — Observação inicial no cadastro de cliente

```
Como um USER_ATTENDANT ou USER_LEADS,
Eu quero registrar uma observação livre no momento de cadastrar o cliente,
Para que o contexto do primeiro contato seja preservado junto ao cadastro.
```

#### Critérios de Aceite

```
Dado que o usuário está criando um novo Customer,
Quando o body do POST /api/v1/customers inclui o campo "initialNote" preenchido,
Então o Customer deve ser persistido com o campo initialNote salvo,
E um ContactLog deve ser criado automaticamente com note=initialNote e channel=informado_no_cadastro.
```

```
Dado que o campo initialNote não é informado no cadastro,
Quando o Customer é criado,
Então o campo initialNote deve ser null — não é obrigatório,
E nenhum ContactLog automático de observação inicial deve ser criado.
```

---

### US-PPR-06 — Métricas de pós-procedimento para o gestor

```
Como um ADM_SYSTEM ou usuário com role de gestor,
Eu quero visualizar a taxa de retorno dos pacientes pós-procedimento,
Para que eu possa medir a fidelização e identificar oportunidades de melhoria no acompanhamento.
```

#### Critérios de Aceite

```
Dado que o usuário autenticado tem permissão para ANALYTICS / READ / GLOBAL,
Quando faz GET /api/v1/analytics/post-procedure?from=2026-01-01&to=2026-05-31,
Então o sistema deve retornar:
  - totalPostProcedure: número de tickets que entraram em POST_PROCEDURE no período
  - returnedCount: quantos progrediram para SCHEDULED
  - lostCount: quantos foram para LOSS
  - returnRate: returnedCount / totalPostProcedure (percentual)
  - pendingCount: quantos ainda estão em POST_PROCEDURE sem transição.
```

```
Dado que não há tickets POST_PROCEDURE no período,
Quando o endpoint é consultado,
Então o sistema deve retornar todos os campos com valor 0 e returnRate=0.0,
E HTTP 200 (não 404).
```

```
Dado que o usuário não tem permissão de ANALYTICS,
Quando tenta acessar o endpoint,
Então o sistema deve retornar HTTP 403.
```

---

## Impacto nos Endpoints Existentes

| Endpoint | Mudança |
|---|---|
| `PATCH /api/v1/tickets/{id}/status` | Aceitar `POST_PROCEDURE`, `SCHEDULED` (com returnScheduledAt), `LOSS` (com lossReason) na transição de POST_PROCEDURE |
| `POST /api/v1/customers` | Aceitar campo opcional `initialNote` |
| `GET /api/v1/customers/{id}` | Retornar `initialNote` no response |
| `GET /api/v1/tickets/{id}` | Retornar `procedurePerformedAt` e `returnScheduledAt` |

## Novos Endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/analytics/post-procedure` | Métricas de pós-procedimento por período |

---

## Regras de Negócio Críticas

1. A transição `WIN → POST_PROCEDURE` só pode ser feita por `USER_ATTENDANT` ou `ADM_SYSTEM`.
2. `ContactLog` é sempre imutável — nenhum UPDATE ou DELETE.
3. A data `procedurePerformedAt` é preenchida automaticamente pelo sistema no momento da transição — não pode ser informada pelo usuário.
4. Um ticket `POST_PROCEDURE → SCHEDULED` reinsere o paciente no ciclo a partir do `EVALUATOR` — não do zero (LEADS).
5. `returnRate` deve usar `BigDecimal` com `RoundingMode.HALF_UP`, 2 casas decimais.

---

## Dependências Técnicas

| Item | Arquivo | Mudança |
|---|---|---|
| Enum | `core/enums/TicketStatus.java` | Adicionar `POST_PROCEDURE` |
| Constante | `funnel/service/impl/LeadTicketServiceImpl.java` | Atualizar `ALLOWED_TRANSITIONS` |
| Entidade | `funnel/domain/model/LeadTicket.java` | Adicionar `procedurePerformedAt`, `returnScheduledAt` |
| Entidade | `funnel/domain/model/Customer.java` | Adicionar `initialNote` |
| DTO Request | `funnel/api/dto/request/leadTicket/LeadTicketChangeStatusRequestDTO.java` | Adicionar `returnScheduledAt`, `lossReason` |
| DTO Request | `funnel/api/dto/request/customer/CustomerCreateRequestDTO.java` | Adicionar `initialNote` |
| DTO Response | `funnel/api/dto/response/LeadTicketResponseDTO.java` | Expor novos campos |
| DTO Response | `funnel/api/dto/response/CustomerResponseDTO.java` | Expor `initialNote` |
| Serviço | `funnel/service/impl/LeadTicketServiceImpl.java` | Lógica de transição para POST_PROCEDURE |
| Serviço | `funnel/service/impl/CustomerServiceImpl.java` | Persistir `initialNote` + ContactLog automático |
| Analytics | `analytics/service/impl/AnalyticsServiceImpl.java` | Novo método `getPostProcedureMetrics()` |
| DTO Result | `analytics/api/dto/PostProcedureResultDTO.java` | Novo DTO de resultado |
| Controller | `analytics/api/controller/AnalyticsController.java` | Novo endpoint GET |
| Migration | `resources/db/migration/` | ALTER TABLE para novos campos |

---

## Definition of Done

- [ ] `POST_PROCEDURE` adicionado ao enum `TicketStatus`
- [ ] `ALLOWED_TRANSITIONS` atualizado com as novas transições
- [ ] Campos `procedurePerformedAt` e `returnScheduledAt` persistidos em `LeadTicket`
- [ ] Campo `initialNote` persistido em `Customer`
- [ ] ContactLog automático gerado em todas as transições de POST_PROCEDURE
- [ ] Validações de negócio implementadas (lossReason obrigatório, returnScheduledAt no futuro)
- [ ] Endpoint de analytics `/post-procedure` funcional com todos os campos especificados
- [ ] Permissões verificadas via `PermissionService.checkOrThrow()` em todos os endpoints
- [ ] Testes unitários cobrindo as transições e validações
- [ ] Migration de banco aplicada sem breaking change

---

*Gerado por: Product Owner Agent*
*Data: 2026-05-27*
*Projeto: OdontoCore CRM — io.sertaoBit.odontocore.crm*
