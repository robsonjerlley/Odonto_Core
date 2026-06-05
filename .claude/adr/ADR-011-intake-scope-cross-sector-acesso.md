# ADR-011: Escopo INTAKE para acesso cross-sector entre setores de captação

**Status**: Aceito  
**Data**: 2026-06-05  
**Autores**: Arquiteto-Agent  
**Impacto**: `PermissionScope.java`, `PermissionService.java`, `PermissionSeeder.java`

---

## Contexto

O sistema possui quatro setores operacionais: `LEADS`, `ATTENDANT`, `EVALUATOR`, `COMMERCIAL`.

`LEADS` e `ATTENDANT` são os dois **setores de captação** (intake):

- `USER_LEADS`: agente de relacionamento remoto — liga, WhatsApp, mídia digital.
- `USER_ATTENDANT`: recepcionista presencial — atende o público na clínica.

O problema identificado em produção: com scope `OWN` para `TICKET:UPDATE` e `CUSTOMER:READ`, cada usuário só acessa o que criou pessoalmente. Isso quebra o seguinte fluxo legítimo:

> Um agente de leads captura um cliente remotamente. Esse cliente aparece presencialmente na clínica. O recepcionista precisa (a) confirmar a identidade do cliente pelos dados de contato e (b) agendar a consulta — mas o ticket foi criado pelo agente de leads, não pelo recepcionista. Com scope OWN, ambas as operações resultam em 403.

O modelo atual tem três escopos: `GLOBAL`, `SECTOR`, `OWN`. Nenhum deles cobre "pode acessar se o recurso está em qualquer um dos dois setores de captação".

- `SECTOR` não resolve: o recepcionista é do setor `ATTENDANT`, o ticket está em `LEADS`.
- `GLOBAL` é excessivo: daria acesso a tickets de `EVALUATOR` e `COMMERCIAL`, violando a separação de setores.

---

## Decisão

Adicionar um quarto valor ao enum `PermissionScope`: **`INTAKE`**.

**Semântica do escopo INTAKE:**

> Um usuário de setor de captação (`LEADS` ou `ATTENDANT`) pode acessar recursos que também estão em um setor de captação (`LEADS` ou `ATTENDANT`). Recursos em setores `EVALUATOR` ou `COMMERCIAL` permanecem bloqueados para esses perfis.

**Implementação em `PermissionService.resolveScope()`:**

```java
case INTAKE -> isIntakeSector(user.getSector()) && isIntakeSector(targetSector);

private boolean isIntakeSector(Sector sector) {
    return sector == Sector.LEADS || sector == Sector.ATTENDANT;
}
```

**Exemplo de comportamento:**

| Usuário (setor) | Recurso (targetSector) | Resultado INTAKE |
|-----------------|------------------------|------------------|
| ATTENDANT       | LEADS ticket           | ✅ permitido      |
| LEADS           | ATTENDANT customer     | ✅ permitido      |
| ATTENDANT       | EVALUATOR ticket       | 🚫 negado         |
| LEADS           | COMMERCIAL deal        | 🚫 negado         |

**A proteção contra transições indevidas permanece nas camadas de negócio:**
`TRANSITION_ROLES` e `ALLOWED_TRANSITIONS` em `LeadTicketServiceImpl` continuam sendo a guarda para quais status transitions cada role pode executar — independente do scope RBAC.

---

## Regras do PermissionSeeder afetadas

### USER_ATTENDANT

| Resource | Action | Antes | Depois | Motivo |
|----------|--------|-------|--------|--------|
| CUSTOMER | READ   | OWN   | INTAKE | ver dados de clientes captados por leads |
| TICKET   | READ   | OWN   | INTAKE | encontrar tickets de leads antes de agendar |
| TICKET   | UPDATE | OWN   | INTAKE | agendar consulta de ticket criado por leads |

### USER_LEADS

| Resource | Action | Antes | Depois | Motivo |
|----------|--------|-------|--------|--------|
| CUSTOMER | READ   | OWN   | INTAKE | ver dados de clientes walk-in do recepcionista |

`TICKET:UPDATE` do `USER_LEADS` **permanece OWN** — agente remoto gerencia sua própria pipeline. Coordenação cross-sector é responsabilidade do `ADM_LEADS`.

### ADM_LEADS

| Resource | Action | Antes  | Depois | Motivo |
|----------|--------|--------|--------|--------|
| CUSTOMER | READ   | SECTOR | INTAKE | visibilidade de clientes de recepção |
| TICKET   | READ   | SECTOR | INTAKE | visão completa do intake para coordenação |
| TICKET   | UPDATE | SECTOR | INTAKE | agendar e gerenciar tickets de ambos setores de captação |

`ADM_LEADS` é o coordenador do intake: pode gerenciar agendamentos de `LEADS` e `ATTENDANT`, mas **nunca** de `EVALUATOR` ou `COMMERCIAL` — o escopo INTAKE bloqueia naturalmente.

---

## Decisão sobre pipeline filter (Fase 3 RBAC)

O escopo INTAKE também define o comportamento do filtro de query em `search()`:

| Escopo do usuário | Filtro SQL aplicado em `search()` |
|-------------------|-----------------------------------|
| GLOBAL            | sem filtro — retorna tudo          |
| SECTOR            | `WHERE current_sector = ?`         |
| INTAKE            | `WHERE current_sector IN ('LEADS', 'ATTENDANT')` |
| OWN               | `WHERE created_by = ?`             |

`PermissionService` deve expor `getScope(user, resource, action)` para que os services consultem o escopo e apliquem o filtro correto nas queries.

---

## Consequências Positivas

- Isolamento de setores mantido: `EVALUATOR` e `COMMERCIAL` não são acessíveis pelo intake.
- Fluxo walk-in corrigido sem exceções hardcoded no service.
- Escopo expressivo e reutilizável — futuras expansões (ex: `CLINIC` cobrindo múltiplos setores) seguem o mesmo padrão.
- Fase 3 RBAC agora tem um contrato claro de implementação via `getScope()`.

## Consequências Negativas / Riscos

- Requer limpeza da tabela `permission_rules` no banco de produção após o deploy, pois o seeder tem early return quando a tabela não está vazia.
- `isIntakeSector()` é uma regra de domínio hardcoded no `PermissionService` — se o negócio criar um terceiro setor de captação no futuro, esse método precisa ser atualizado junto com o seeder.
- O `targetSector` em `CustomerServiceImpl.search()` e `findByCpf()` é passado como `user.getSector()` (não o setor do customer). Para INTAKE, isso significa que o check sempre passa para usuários de intake, mas o filtro SQL (Fase 3) precisa usar a tabela `users` para determinar o setor do criador do customer.

## Alternativas Consideradas

- **GLOBAL scope para USER_ATTENDANT/USER_LEADS** — descartado. Daria acesso a tickets e clientes de `EVALUATOR`/`COMMERCIAL`, violando a separação de setores definida pelo negócio.
- **Exceção hardcoded em `changeStatus()`** — descartado. Seria um bypass do RBAC, não uma extensão dele. Tornaria a lógica de autorização invisível e difícil de auditar.
- **Novo setor `INTAKE` agrupando LEADS e ATTENDANT** — descartado. Exigiria migração de schema, reprocessamento de todos os usuários e tickets existentes, e quebraria o modelo de usuário atual.

---

## Referências Cruzadas

- `ADR-004` — padrão de chamada ao `checkOrThrow`, Padrão 3 (SEARCH) agora tem filtro SQL definido
- `PermissionService.java` — implementação de `resolveScope()` e novo `getScope()`
- `PermissionScope.java` — enum a ser atualizado
- `PermissionSeeder.java` — matriz RBAC com novas regras INTAKE
- `bugs-producao-railway-2026-06-05.md` — bugs #2, #6, #10, #11, #17 resolvidos por este ADR