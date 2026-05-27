# US — Fundacional: Cadastro Mínimo e Permissões de Atendente

**Épico:** Base de dados completa e controle de acesso correto por perfil
**Prioridade:** Must Have — implementar ANTES do us-pos-procedimento.md
**Status:** Backlog — aguardando implementação

---

## Contexto e Motivação

Duas falhas foram identificadas na base do sistema antes da implementação do módulo pós-procedimento:

1. **CPF obrigatório** impede o registro de contatos que não chegaram a fornecer dados completos, causando perda permanente de leads não convertidos.
2. **USER_ATTENDANT com permissões incorretas** — o atendente local (inbound) está equiparado ao agente de leads (outbound), o que é incorreto operacionalmente e representa risco de acesso indevido.

Estes problemas afetam o ponto de entrada da esteira. Devem ser corrigidos antes de qualquer extensão do fluxo.

---

## US-FUND-01 — CPF Opcional e Registro de Contato Inicial

```
Como um USER_ATTENDANT ou USER_LEADS,
Eu quero cadastrar um cliente com apenas nome e telefone (sem CPF obrigatório),
Para que contatos não convertidos sejam registrados no sistema e possam ser retomados futuramente.
```

### Motivação do Negócio

O CPF só é fornecido quando o cliente tem interesse real no tratamento. Em um primeiro contato (ligação curta, visita rápida), o lead pode não chegar a fornecer CPF — mas os dados do contato têm valor para tentativas futuras e para métricas de captação.

### Regras de Negócio

1. `cpf` passa a ser **opcional** em `Customer` — `nullable = true`.
2. A constraint `UNIQUE` em `cpf` é **mantida** — dois clientes com o mesmo CPF não são permitidos. Dois clientes com `cpf = null` **são permitidos** (comportamento padrão de UNIQUE + NULL no PostgreSQL: `NULL ≠ NULL`).
3. `phone` deve ser o campo de deduplicação prático — o sistema deve permitir busca por telefone antes de criar novo cadastro para evitar duplicatas.
4. `initialNote` é um campo **opcional** de texto livre capturado no momento do cadastro — representa a observação do primeiro contato.
5. Quando `initialNote` está preenchido, o sistema deve criar um `ContactLog` automaticamente após salvar o Customer e o LeadTicket.
6. Quando `initialNote` está ausente, nenhum ContactLog automático de observação inicial é criado.
7. Um contato com dados insuficientes (sem nome e sem telefone) **não deve ser registrado** — dado mínimo identificável é obrigatório.
8. **CPF é obrigatório para formalização do agendamento.** A transição de qualquer ticket para o status `SCHEDULED` deve ser bloqueada se o `Customer` associado não possuir CPF preenchido. Esta invariante garante que nenhum `Deal` (criado após SCHEDULED) exista sem identificação única do paciente — requisito crítico para o módulo financeiro futuro.
9. O sistema deve permitir que o CPF seja adicionado a um Customer existente via `UPDATE` a qualquer momento antes do agendamento, sem recriar o cadastro.

---

### Critérios de Aceite

```
Dado que um USER_ATTENDANT está cadastrando um cliente que não forneceu CPF,
Quando faz POST /api/v1/customers com nome e telefone preenchidos e cpf ausente,
Então o Customer deve ser criado com cpf = null,
E um LeadTicket deve ser aberto automaticamente com status NEW,
E a resposta deve ser HTTP 201 com os dados do Customer criado.
```

```
Dado que já existe um Customer com cpf = "123.456.789-00",
Quando um novo cadastro é tentado com o mesmo CPF,
Então o sistema deve retornar HTTP 409 com mensagem "CPF já cadastrado.".
```

```
Dado que já existem dois Customers com cpf = null,
Quando um terceiro cadastro é feito também sem CPF,
Então o sistema deve criar o Customer normalmente — múltiplos nulls são permitidos.
```

```
Dado que o usuário está cadastrando um cliente e preencheu o campo initialNote,
Quando o POST /api/v1/customers é processado,
Então o Customer deve ser salvo com o campo initialNote,
E um ContactLog deve ser criado automaticamente com:
  - ticketId = id do LeadTicket recém-criado
  - userId = id do usuário autenticado
  - channel = canal informado no cadastro
  - note = valor do initialNote
  - occurredAt = momento da criação.
```

```
Dado que o usuário está cadastrando um cliente sem preencher initialNote,
Quando o POST /api/v1/customers é processado,
Então o Customer deve ser salvo com initialNote = null,
E nenhum ContactLog automático deve ser criado nesse momento.
```

```
Dado que o usuário tenta cadastrar um cliente sem nome e sem telefone,
Quando o POST /api/v1/customers é processado,
Então o sistema deve retornar HTTP 400 com mensagem de validação para os campos obrigatórios.
```

```
Dado que o usuário deseja verificar se um telefone já está cadastrado antes de criar novo Customer,
Quando faz GET /api/v1/customers?phone=+5511999999999,
Então o sistema deve retornar a lista de Customers com aquele telefone (pode ser vazia),
E HTTP 200 em qualquer caso.
```

```
Dado que existe um LeadTicket com Customer que possui cpf = null,
Quando o usuário tenta fazer PATCH /api/v1/tickets/{id}/status com body { "status": "SCHEDULED" },
Então o sistema deve retornar HTTP 422 com mensagem "CPF obrigatório para formalização do agendamento.",
E o status do ticket não deve ser alterado.
```

```
Dado que existe um Customer com cpf = null,
Quando o usuário faz PUT/PATCH /api/v1/customers/{id} informando um CPF válido,
Então o CPF deve ser persistido no Customer,
E o Customer deve estar apto para agendamento a partir deste momento.
```

```
Dado que existe um Customer com cpf = null,
Quando o usuário faz PUT/PATCH /api/v1/customers/{id} informando um CPF que já pertence a outro Customer,
Então o sistema deve retornar HTTP 409 com mensagem "CPF já cadastrado.",
E o Customer não deve ser atualizado.
```

```
Dado que existe um LeadTicket com Customer que possui CPF preenchido,
Quando o usuário faz PATCH /api/v1/tickets/{id}/status com body { "status": "SCHEDULED" },
Então o sistema deve processar a transição normalmente.
```

---

### Impacto no Domínio

| Item | Arquivo | Mudança |
|---|---|---|
| Entidade | `funnel/domain/model/Customer.java` | `cpf`: remover `unique = true` inline, manter via `@Table(uniqueConstraints)` com nullable; adicionar `initialNote TEXT nullable` |
| DTO Request | `funnel/api/dto/request/customer/CustomerCreateRequestDTO.java` | Remover `@NotBlank` do `cpf`; adicionar campo opcional `initialNote` |
| DTO Response | `funnel/api/dto/response/CustomerResponseDTO.java` | Expor `initialNote` |
| Serviço | `funnel/service/impl/CustomerServiceImpl.java` | Fix NPE em `update()` com `Objects.equals()`; injetar `ContactLogRepository`; lógica de ContactLog automático quando `initialNote != null` |
| Serviço | `funnel/service/impl/LeadTicketServiceImpl.java` | Validar CPF não-nulo antes de transição para `SCHEDULED` — busca Customer pelo `ticket.customerId` |
| Repositório | `funnel/repository/CustomerRepository.java` | Adicionar `findByPhone(String phone)` |
| Controller | `funnel/api/controller/CustomerController.java` | Adicionar endpoint `GET /customers?phone=` |
| Migration | `resources/db/migration/` | V1: `ALTER TABLE customers ALTER COLUMN cpf DROP NOT NULL` + `ALTER TABLE customers ADD COLUMN initial_note TEXT` |

---

### Definition of Done — US-FUND-01

- [ ] `cpf` nullable na entidade e na migration (V1)
- [ ] Constraint UNIQUE mantida via `@Table(uniqueConstraints)`
- [ ] `initialNote` (TEXT) adicionado em Customer, DTO Request e DTO Response
- [ ] ContactLog automático criado quando `initialNote` presente
- [ ] Endpoint de busca por phone funcional (`GET /customers?phone=`)
- [ ] Fix NPE em `CustomerServiceImpl.update()` usando `Objects.equals()`
- [ ] Validação CPF obrigatório em `LeadTicketServiceImpl.changeStatus()` para transição → SCHEDULED
- [ ] HTTP 422 retornado corretamente quando CPF ausente no agendamento
- [ ] `CustomerUpdateRequestDTO` suporta atualização de CPF com validação de unicidade
- [ ] Testes unitários: CPF null no cadastro, ContactLog automático, bloqueio de SCHEDULED sem CPF

---

## US-FUND-02 — Revisão de Permissões do USER_ATTENDANT

```
Como gestor do sistema (ADM_SYSTEM),
Eu quero que o perfil USER_ATTENDANT tenha acesso restrito às operações de atendimento local,
Para que o atendente não acesse funcionalidades destinadas ao time de captação de leads (USER_LEADS).
```

### Motivação do Negócio

O `USER_ATTENDANT` é o **atendente local** da clínica: recebe pacientes que chegam presencialmente ou ligam diretamente para a clínica. Ele não prospecta, não qualifica leads via redes sociais e não toma decisões sobre perda de oportunidades — esse papel é do `USER_LEADS`.

Equiparar os dois perfis é um risco operacional: o atendente poderia alterar status de tickets de forma indevida, ver métricas de captação que não são de sua alçada ou tomar decisões que cabem ao setor de leads.

### Distinção de Papéis

| Dimensão | USER_LEADS | USER_ATTENDANT |
|---|---|---|
| Origem do contato | Outbound — busca o cliente | Inbound — cliente chega |
| Canal típico | Redes sociais, ligações ativas | Presencial, telefone da clínica |
| Responsabilidade principal | Qualificar e converter leads | Receber, cadastrar e agendar |
| Métricas acessíveis | Pessoais + setor | Apenas pessoais |
| Decisão de perda (LOSS) | Sim | Não |

### Matriz de Permissões — USER_ATTENDANT

| Resource | Action | Scope | Permitido | Observação |
|---|---|---|---|---|
| CUSTOMER | CREATE | OWN | ✅ | Cadastra clientes que chegam |
| CUSTOMER | READ | OWN | ✅ | Consulta clientes que cadastrou |
| CUSTOMER | UPDATE | OWN | ✅ | Atualiza dados básicos |
| CUSTOMER | DELETE | — | 🚫 | Nunca |
| TICKET | READ | OWN | ✅ | Consulta tickets vinculados a si |
| TICKET | CREATE | — | 🚫 | Sistema cria automaticamente ao criar Customer |
| TICKET | UPDATE → SCHEDULED | OWN | ✅ | Agenda consulta |
| TICKET | UPDATE → POST_PROCEDURE | OWN | ✅ | Marca procedimento realizado |
| TICKET | UPDATE → LOSS | — | 🚫 | Decisão de perda cabe ao LEADS |
| TICKET | UPDATE → IN_CONTACT | — | 🚫 | Operação de leads outbound |
| CONTACT_LOG | CREATE | OWN | ✅ | Registra observações de contato |
| CONTACT_LOG | READ | OWN | ✅ | Consulta histórico dos seus tickets |
| DEAL | READ | — | 🚫 | Orçamento não é responsabilidade do atendente |
| DEAL | CREATE/UPDATE | — | 🚫 | Papel do avaliador e comercial |
| ANALYTICS | READ (pessoal) | OWN | ✅ | Métricas do próprio atendente |
| ANALYTICS | READ (setor) | SECTOR | 🚫 | Não vê métricas de outros |
| CONFIG | * | — | 🚫 | Configurações são do gestor |
| USER | * | — | 🚫 | Gestão de usuários é do ADM |

---

### Critérios de Aceite

```
Dado que um usuário com role USER_ATTENDANT está autenticado,
Quando tenta fazer POST /api/v1/tickets (criar ticket manualmente),
Então o sistema deve retornar HTTP 403.
```

```
Dado que um usuário com role USER_ATTENDANT está autenticado,
Quando tenta fazer PATCH /api/v1/tickets/{id}/status com body { "status": "LOSS" },
Então o sistema deve retornar HTTP 403.
```

```
Dado que um usuário com role USER_ATTENDANT está autenticado,
Quando tenta fazer PATCH /api/v1/tickets/{id}/status com body { "status": "SCHEDULED" } em um ticket que lhe está atribuído,
Então o sistema deve processar normalmente e retornar HTTP 200.
```

```
Dado que um usuário com role USER_ATTENDANT está autenticado,
Quando tenta fazer PATCH /api/v1/tickets/{id}/status com body { "status": "POST_PROCEDURE" } em um ticket WIN,
Então o sistema deve processar normalmente e retornar HTTP 200.
```

```
Dado que um usuário com role USER_ATTENDANT está autenticado,
Quando tenta acessar GET /api/v1/analytics/sector,
Então o sistema deve retornar HTTP 403.
```

```
Dado que um usuário com role USER_ATTENDANT está autenticado,
Quando acessa GET /api/v1/analytics/personal,
Então o sistema deve retornar HTTP 200 com as métricas pessoais do próprio atendente.
```

```
Dado que um usuário com role USER_ATTENDANT está autenticado,
Quando tenta acessar GET /api/v1/deals/{id},
Então o sistema deve retornar HTTP 403.
```

---

### Impacto no Domínio

| Item | Arquivo | Mudança |
|---|---|---|
| Seeder | `identity/config/PermissionSeeder.java` | Revisar e corrigir as regras inseridas para `Role.USER_ATTENDANT` |
| Permissões | `identity/domain/model/PermissionRule.java` | Nenhuma mudança estrutural — apenas os dados do seeder |
| Security | `config/security/SecurityConfig.java` | Verificar se há regras hardcoded para ATTENDANT que contradizem a matriz |

> **Nota:** A implementação é majoritariamente no `PermissionSeeder` — não requer novos endpoints ou entidades. Esforço estimado: 0.2 person-months.

---

### Definition of Done — US-FUND-02

- [ ] `PermissionSeeder` atualizado com a matriz completa para `USER_ATTENDANT`
- [ ] Verificar que `SecurityConfig` não possui regras que contradigam a matriz
- [ ] Testes de integração validando os critérios de aceite (403 para LOSS, 200 para SCHEDULED)
- [ ] Confirmar que USER_LEADS mantém suas permissões inalteradas

---

## Ordem de Implementação Recomendada

```
US-FUND-01 (CPF opcional + initialNote)
    ↓
US-FUND-02 (Permissões ATTENDANT)
    ↓
US-PPR-01  (POST_PROCEDURE — ver us-pos-procedimento.md)
```

**Justificativa:** US-FUND-01 afeta o modelo de dados base — deve ser migrado antes de qualquer nova feature. US-FUND-02 afeta quem pode fazer o quê, incluindo a transição WIN → POST_PROCEDURE do US-PPR-01.

---

*Gerado por: Product Owner Agent*
*Data: 2026-05-27*
*Projeto: OdontoCore CRM — io.sertaoBit.odontocore.crm*
*Referência cruzada: us-pos-procedimento.md*
