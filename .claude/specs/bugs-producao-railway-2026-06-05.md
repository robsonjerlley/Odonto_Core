# Bugs Produção Railway — 2026-06-05

**Origem:** revisão após primeiro deploy no Railway  
**Total de itens:** 17  
**Status geral:** Backlog — aguardando implementação  
**Prioridade geral:** Must Have (itens #1–#17 parcialmente bloqueadores de operação)

---

## Índice por Área

| Área | Itens | Severidade |
|------|-------|------------|
| Backend — RBAC / PermissionSeeder | #1, #2, #6, #11, #14, #16, #17 | 🔴 Bloqueador |
| Backend — Business Logic / API | #5, #10, #15 | 🔴/🟡 Crítico |
| Frontend — UI/UX | #3, #7, #8, #9, #12 | 🟡/🟢 Médio |
| Feature nova (backlog) | #4, #13 | 🟢 Backlog |

---

## MoSCoW desta Release

**Must Have** — bloqueia operação em produção:
`#2`, `#5`, `#6`, `#11`, `#12`, `#14`, `#15`, `#17`

**Should Have** — degrada operação:
`#1`, `#10`, `#16`, `#9`

**Could Have** — polimento:
`#3`, `#7`, `#8`

**Backlog** — features novas:
`#4`, `#13`

---

## BLOCO BACKEND — RBAC / PermissionSeeder

### #1 — Avaliador vê o pipeline inteiro

**Descrição:** `USER_EVALUATOR` acessa a aba Pipeline, que é a visão de gestão comercial de leads — o avaliador não deve ter essa visão.

**Causa raiz:** O seeder concede `TICKET:READ (SECTOR)` e `TICKET:UPDATE (SECTOR)` ao `USER_EVALUATOR`, o que é correto para o fluxo de avaliação. O problema é de **navegação no frontend**: a aba "Pipeline" é renderizada para todos que têm TICKET:READ.

**Solução:**
- Backend: manter permissões de TICKET (necessárias para avaliação) — sem alteração.
- Frontend: não renderizar a aba "Pipeline" para roles do setor `EVALUATOR`. Exibir apenas aba "Avaliações".

**Arquivos relacionados:**
- `PermissionSeeder.java` — linhas 128–130, 140–142 (regras USER_EVALUATOR)

**Critério de aceite:**
```
Dado que o usuário é USER_EVALUATOR,
Quando acessar o sistema,
Então não deve ver a aba "Pipeline" — apenas "Avaliações" e "Clientes".
```

---

### #2 — Nome do paciente some para avaliador, comercial e outros perfis

**Descrição:** Em telas de negociações e tickets, o nome do paciente não é exibido para usuários comerciais e avaliadores.

**Causa raiz:** `ADM_COMMERCIAL` e `USER_COMMERCIAL` **não possuem nenhuma regra para o resource `CUSTOMER`** no `PermissionSeeder`. A chamada `GET /customers/{id}` retorna 403, impedindo o frontend de exibir o nome.

**Solução — PermissionSeeder.java:** adicionar:
```java
// ADM_COMMERCIAL
rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, CUSTOMER, READ, SECTOR));

// USER_COMMERCIAL
rules.add(rule(USER_COMMERCIAL, COMMERCIAL, CUSTOMER, READ, OWN));
```

**Observação:** `USER_EVALUATOR` já tem `CUSTOMER:READ (GLOBAL)` — não requer alteração.

**Critério de aceite:**
```
Dado que o usuário é ADM_COMMERCIAL ou USER_COMMERCIAL,
Quando visualizar um deal ou ticket,
Então o nome do paciente deve ser exibido corretamente.
```

---

### #6 — 403 ao acessar aba Customers para perfis comerciais

**Descrição:** Usuários do setor comercial recebem 403 ao abrir a aba de clientes.

**Causa raiz:** Mesma que #2 — `ADM_COMMERCIAL` e `USER_COMMERCIAL` sem `CUSTOMER:READ` no seeder.

**Solução:** mesma correção do item #2 resolve este bug.

---

### #11 — Coordenador clínico: 403 universal em pipeline, clientes e avaliações

**Descrição:** Usuários com perfil de Coordenador Clínico recebem 403 em todas as abas.

**Causa raiz:** O role `COORDENADOR_CLINICO` (ou equivalente) **não existe no enum `Role.java`**. Nenhuma regra de permissão pode ser vinculada a esse perfil.

**Solução:**
1. Adicionar novo valor ao enum `Role.java`:
   ```java
   ADM_SYSTEM, ADM_LEADS, USER_LEADS, USER_ATTENDANT,
   ADM_EVALUATOR, USER_EVALUATOR, ADM_COMMERCIAL, USER_COMMERCIAL,
   ADM_CLINICAL  // novo
   ```
2. Adicionar ao `PermissionSeeder.java` — permissões de leitura para o coordenador:
   ```java
   // ADM_CLINICAL — visão de leitura global (sem poder criar/editar)
   rules.add(rule(ADM_CLINICAL, null, CUSTOMER, READ, GLOBAL));
   rules.add(rule(ADM_CLINICAL, null, TICKET,   READ, GLOBAL));
   rules.add(rule(ADM_CLINICAL, null, DEAL,     READ, GLOBAL));
   rules.add(rule(ADM_CLINICAL, null, ANALYTICS, READ, GLOBAL));
   ```

**Padrão de mercado:** em CRMs odontológicos, o coordenador clínico tem visão de leitura ampla (agenda, pacientes, avaliações) sem permissão operacional de criação ou edição.

**Critério de aceite:**
```
Dado que o usuário é ADM_CLINICAL,
Quando acessar pipeline, clientes ou avaliações,
Então deve visualizar os dados sem receber 403,
E não deve conseguir criar, editar ou excluir registros.
```

---

### #14 — Gerente de relacionamento: 403 em negociações e ao aplicar desconto

**Descrição:** `ADM_COMMERCIAL` recebe 403 ao abrir a aba de negociações e ao tentar aplicar desconto em um deal.

**Causa raiz — dupla:**

1. **`DealServiceImpl.applyDiscount()` usa `Action.CONFIGURE`** (linha 175), mas o seeder não possui regra `DEAL:CONFIGURE` para nenhum role comercial. O `checkOrThrow` lança 403 imediatamente.

2. **Scope OWN com `deal.getCreatedBy()`** em `getDealWithHistory()` e `closeDeal()`: o deal é criado pelo avaliador, então `deal.createdBy` = ID do avaliador ≠ ID do comercial. A checagem OWN falha para o comercial.

**Solução:**
- `DealServiceImpl.java` linha 175: trocar `CONFIGURE` por `UPDATE`:
  ```java
  permissionService.checkOrThrow(user, DEAL, UPDATE, user.getSector(), deal.getCreatedBy());
  ```
- Para READ/CLOSE com scope OWN: revisar se o ownerId deve ser `deal.createdBy` ou `currentUser.getId()`. O comercial que fecha o deal pode não ser quem o criou — considerar scope `SECTOR` para `ADM_COMMERCIAL`.

**Critério de aceite:**
```
Dado que o usuário é ADM_COMMERCIAL,
Quando acessar a aba negociações,
Então deve listar os deals do setor sem 403.

Dado que o usuário é ADM_COMMERCIAL,
Quando aplicar um desconto em um deal,
Então o desconto deve ser aplicado sem erro 403.
```

---

### #16 — Atendente: 403 no dashboard de performance + dados demais exibidos

**Descrição:** `USER_ATTENDANT` tem permissão `ANALYTICS:READ (OWN)` no seeder mas recebe 403 ao abrir o dashboard. Além disso, o dashboard exibe métricas financeiras (conversão, ticket médio, caixa esperado) que o atendente não deve ver.

**Causa raiz — dupla:**
1. O analytics service provavelmente não implementa o scope `OWN` corretamente, ou o endpoint exige `GLOBAL`.
2. O frontend renderiza todas as métricas independente do role.

**Solução:**
- Backend: corrigir o analytics service para aceitar scope `OWN` (retorna apenas dados do próprio usuário).
- Frontend: exibir métricas filtradas por role:
  - `USER_ATTENDANT` / `USER_LEADS`: atendimentos do dia, leads contatados, agendamentos realizados.
  - `ADM_SECTOR` / `ADM_SYSTEM`: todas as métricas incluindo conversão, ticket médio e caixa esperado.

**Padrão de mercado:** dashboards de CRM usam role-based metric visibility — operadores veem volume de trabalho, gestores veem métricas financeiras e de conversão.

**Critério de aceite:**
```
Dado que o usuário é USER_ATTENDANT,
Quando acessar o dashboard de performance,
Então deve carregar sem 403,
E não deve visualizar campos: conversão, ticket médio, caixa esperado.

Dado que o usuário é ADM_SYSTEM ou ADM_LEADS,
Quando acessar o dashboard,
Então deve visualizar todas as métricas.
```

---

### #17 — Atendente não consegue marcar cliente para consulta

**Descrição:** `USER_ATTENDANT` não consegue realizar o agendamento (transição para status `SCHEDULED`).

**Causa raiz:** `USER_ATTENDANT` tem `TICKET:UPDATE (OWN)` — mas o ticket pode ter sido transferido do setor de leads para o atendente. Nesse caso, `ticket.createdBy` ≠ `attendant.id`, e o scope OWN falha no `checkOrThrow`.

**Solução:** alterar o scope de `TICKET:UPDATE` para `USER_ATTENDANT` de `OWN` para `SECTOR` no `PermissionSeeder`:
```java
// antes:
rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, UPDATE, OWN));
// depois:
rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, UPDATE, SECTOR));
```

**Observação:** verificar se `TICKET:READ` do `USER_ATTENDANT` também precisa de scope `SECTOR` para que o atendente enxergue os tickets transferidos para seu setor.

**Critério de aceite:**
```
Dado que um ticket foi criado pelo setor de leads e transferido para o atendente,
Quando o USER_ATTENDANT tentar agendar uma consulta (SCHEDULED),
Então a transição de status deve ser aceita sem 403.
```

---

## BLOCO BACKEND — Business Logic / API

### #5 — Troca de senha não valida senha atual

**Descrição:** O endpoint de `changePassword` aceita a nova senha sem verificar se a senha atual informada está correta.

**Causa raiz:** Vulnerabilidade de segurança — ausência de validação da senha atual no `AuthService`.

**Solução:** O DTO de troca de senha deve incluir `currentPassword`. O service deve verificar via `passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())` antes de aceitar a nova senha. Se não corresponder, retornar `400 Bad Request` com mensagem `"Senha atual incorreta"`.

**Padrão de mercado:** `{ currentPassword, newPassword, confirmNewPassword }` — os três campos são obrigatórios.

**Critério de aceite:**
```
Dado que o usuário informa uma senha atual incorreta,
Quando solicitar a troca de senha,
Então o sistema deve retornar 400 com mensagem "Senha atual incorreta".

Dado que o usuário informa a senha atual correta,
Quando solicitar a troca de senha,
Então a senha deve ser atualizada com sucesso.
```

---

### #10 — Atendente/lead não consegue agendar cliente criado por outro setor

**Descrição:** Clientes cadastrados por um setor diferente não aparecem na busca do atendente para agendamento.

**Causa raiz:** Fase 3 do RBAC (débito técnico documentado em `security-gaps-funnel-permission.md`). O `search()` de Customer filtra por `createdBy = user.id` (scope OWN), bloqueando clientes que chegaram via transferência.

**Decisão de produto:** para o fluxo de **agendamento**, o atendente deve poder localizar qualquer cliente da base que esteja no status adequado. A restrição OWN faz sentido para criação/edição — não para consulta de agendamento.

**Solução recomendada:** endpoint dedicado `GET /customers/search-for-scheduling` sem filtro de `createdBy`, ou parâmetro `context=scheduling` que relaxa o scope para leitura.

**Critério de aceite:**
```
Dado que um cliente foi criado pelo setor de leads,
Quando o USER_ATTENDANT buscar esse cliente para agendamento,
Então deve encontrá-lo na busca e conseguir prosseguir com o agendamento.
```

---

### #15 — 500 Internal Server Error ao fechar deal (`PATCH /deals/{id}/status`)

**Descrição:** `PATCH /deals/{id}/status` retorna 500 ao tentar fechar um deal.

**Causa raiz provável — dois candidatos em `DealServiceImpl.closeDeal()`:**
1. **Scope OWN com `deal.getCreatedBy()`** (linha 223): deal criado pelo avaliador → `deal.createdBy` ≠ `commercial.id` → `checkOrThrow` lança `AccessDeniedException` que não está sendo capturada pelo `GlobalExceptionHandler` como 403, retornando 500.
2. **`dealHistoryService.record(saved.getId(), user, null, saved.getClosedAt())`**: se `DealHistoryService` não aceita `null` como `before`, lança NPE → 500.

**Solução:**
1. Verificar se `GlobalExceptionHandler` mapeia `AccessDeniedException` → `403`.
2. Verificar se `DealHistoryService.record()` trata `null` no parâmetro `before`.
3. Revisar o scope de `DEAL:CLOSE` para `ADM_COMMERCIAL`: trocar `deal.getCreatedBy()` por `user.getId()` ou ampliar para scope `SECTOR`.

**Critério de aceite:**
```
Dado que o usuário é ADM_COMMERCIAL ou USER_COMMERCIAL com permissão DEAL:CLOSE,
Quando fechar um deal com paymentMethod válido,
Então o deal deve ser fechado com status 200,
E o ticket associado deve transitar para WIN.
```

---

## BLOCO FRONTEND — UI/UX

### #3 — Painel de métricas individuais não exibe dados

**Descrição:** A tela de performance pessoal existe no frontend mas não exibe dados.

**Causa raiz:** O endpoint `GET /analytics/...` existe no `AnalyticsController` mas o frontend não está realizando a chamada ou não está conectando a resposta ao componente.

**Solução:** conectar o componente de dashboard ao endpoint de analytics individual. Verificar se o token está sendo enviado na chamada.

---

### #7 — Caixas de diálogo excedem as bordas

**Descrição:** Em selects com opções longas (ex: "Consultor(a) de Relacionamento"), o texto extrapola as bordas do componente.

**Causa raiz:** Ausência de `overflow: hidden`, `text-overflow: ellipsis` ou `max-width` no componente de select/dropdown.

**Solução:** aplicar `truncate` (Tailwind) ou `text-overflow: ellipsis; overflow: hidden; white-space: nowrap` nos itens de select.

---

### #8 — Placeholders com dados reais nos inputs

**Descrição:** Campos de formulário exibem dados fictícios como placeholder (ex: "João Silva", número de CPF de exemplo).

**Causa raiz:** Dados hardcoded nos atributos `placeholder` dos inputs.

**Solução:** substituir por textos descritivos neutros:
- Nome: `"Ex: Maria Santos"` → `"Nome completo"`
- CPF: remover o número de exemplo → `"000.000.000-00"` (máscara visual apenas)

---

### #9 — CPF duplicado: erro não exibido ao clicar em Salvar

**Descrição:** Ao tentar agendar com um CPF já cadastrado, nenhuma mensagem é exibida ao clicar em Salvar. A mensagem só aparece ao fechar a caixa de diálogo.

**Causa raiz:** A resposta de erro da API (409 Conflict) não está sendo tratada no handler do `onSubmit`. O erro provavelmente está sendo capturado apenas no `onClose` do dialog.

**Solução:** no `onSubmit`, capturar o erro da chamada e exibir a mensagem de erro inline (ex: toast ou campo de erro no form) sem fechar o dialog.

**Critério de aceite:**
```
Dado que o CPF informado já existe na base,
Quando o usuário clicar em Salvar,
Então uma mensagem de erro deve ser exibida imediatamente no formulário,
E o diálogo não deve fechar automaticamente.
```

---

### #12 — Valor monetário formatado incorretamente: `5.000,00` vira `5`

**Descrição:** Ao criar um orçamento, valores no formato brasileiro `5.000,00` são enviados incorretamente ao backend, resultando em `5` após salvar.

**Causa raiz:** O frontend envia a string `"5.000,00"` para o backend. O parser (JavaScript `parseFloat` ou Spring `BigDecimal`) interpreta o ponto como separador decimal, resultando em `5.0`.

**Solução:** usar uma biblioteca de máscara monetária que converte a string formatada para valor numérico antes do submit:
- Opções: `react-number-format`, `imask`, `cleave.js`
- A conversão deve remover separadores de milhar e trocar vírgula por ponto antes de enviar: `"5.000,00"` → `5000.00`

**Critério de aceite:**
```
Dado que o usuário digita "5.000,00" no campo de valor,
Quando salvar o procedimento,
Então o valor armazenado e exibido deve ser R$ 5.000,00.
```

---

## BLOCO FEATURE NOVA (Backlog)

### #4 — Associar paciente a avaliador no momento do agendamento

**Descrição:** Ao agendar uma consulta (status `SCHEDULED`), deve ser possível indicar qual avaliador irá atender o paciente.

**Impacto no domínio:**
- Backend: adicionar campo `evaluatorId: UUID` (opcional) ao DTO de `changeStatus` para a transição `SCHEDULED`.
- `LeadTicket`: novo campo `assignedEvaluatorId`.
- Migration de schema necessária.
- Frontend: dropdown de avaliadores disponíveis no formulário de agendamento.

**RICE Score:**
- Reach: 100% dos agendamentos | Impact: 2 (Alto) | Confidence: 80% | Effort: 0.5
- **RICE = 320** — alta prioridade após Must Have

---

### #13 — Campo valor de procedimento: especificar se é unitário ou total

**Descrição:** O campo de valor em procedimentos não indica se o preço é por unidade (ex: implante por dente = R$ 1.500,00/dente) ou total (ex: clareamento = R$ 800,00 o pacote).

**Impacto no domínio:**
- Backend: novo campo `priceType: UNIT | TOTAL` em `DealProcedure`.
- Migration de schema necessária (default `UNIT` para registros existentes).
- Frontend: selector ao lado do campo de valor.

**Padrão de mercado odontológico:** procedimentos como implantes são cobrados por dente (UNIT); tratamentos como aparelho e alinhadores são por pacote/fase (TOTAL).

---

## Dependências e Ordem de Implementação Recomendada

```
Sprint 1 — Backend RBAC (máximo impacto, um único arquivo):
  1. Role.java         → adicionar ADM_CLINICAL
  2. PermissionSeeder  → #2/#6 (CUSTOMER:READ comercial) + #11 (ADM_CLINICAL) + #17 (TICKET:UPDATE SECTOR)
  3. DealServiceImpl   → #14 (CONFIGURE → UPDATE) + #15 (closeDeal scope/NPE)
  4. AuthService       → #5 (validar senha atual)

Sprint 2 — Backend Business Logic + Frontend crítico:
  5. AnalyticsService  → #16 (scope OWN + role-based metrics)
  6. CustomerService   → #10 (endpoint search-for-scheduling)
  7. Frontend          → #12 (currency mask) + #9 (error handling CPF) + #1 (navigation por role)

Sprint 3 — Polimento + Features:
  8. Frontend          → #3, #7, #8, #16 (dashboard role-based)
  9. Feature           → #4 (evaluatorId no agendamento)
  10. Feature          → #13 (priceType em procedimento)
```

---

## Referências Cruzadas

- `PermissionSeeder.java` — fonte de verdade da matriz RBAC
- `DealServiceImpl.java` — origem dos bugs #14 e #15
- `security-gaps-funnel-permission.md` — Fase 3 RBAC (relacionada ao #10)
- `frontend-integration-contract.md` — contratos de API para o frontend
- `ADR-004` — padrão `checkOrThrow` (aplicável às correções de RBAC)