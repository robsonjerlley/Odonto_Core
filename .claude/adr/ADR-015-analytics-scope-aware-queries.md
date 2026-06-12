# ADR-015: Analytics — visibilidade por escopo e refatoração de métodos públicos

**Status**: Aceito  
**Data**: 2026-06-11 (reescrita em 2026-06-11)  
**Autores**: Arquiteto-Agent  
**Impacto**: `AnalyticsService`, `AnalyticsServiceImpl`, `AnalyticsController`  
**Relaciona**: ADR-012 (RBAC list vs single), ADR-013 (Specifications scope-aware)

---

## Contexto

Todos os métodos de `AnalyticsServiceImpl` (exceto `getUserPerformance`) chamam:

```java
permissionService.checkOrThrow(currentUser, ANALYTICS, READ, null, null);
```

Com `null, null`, o `PermissionService.resolveScope()` reprova qualquer role com escopo `SECTOR` ou `OWN` — porque a comparação `user.sector == null` e `user.id == null` sempre falha. Resultado: `403` para todos os papéis exceto `ADM_SYSTEM`.

### Matriz ANALYTICS:READ no seeder (estado atual)

| Role | Scope |
|---|---|
| ADM_SYSTEM | GLOBAL |
| ADM_LEADS | SECTOR |
| ADM_EVALUATOR | SECTOR |
| ADM_COMMERCIAL | SECTOR |
| USER_LEADS | OWN |
| USER_ATTENDANT | OWN |
| USER_EVALUATOR | OWN |
| USER_COMMERCIAL | OWN |

### Por que a versão anterior desta ADR estava errada

A versão anterior propunha um switch genérico `(scope, sector, userId) → query` como padrão único para todos os endpoints. Esse modelo:
- Ignorou que os dados de analytics têm semânticas diferentes por método
- Propôs `getDashboardBySector()` como método novo, que não faz sentido — não há um "dashboard por setor" análogo ao global
- Tentou tornar cada método scope-aware individualmente, gerando redundância e `checkOrThrow` em cascata quando chamados pelo orquestrador

---

## Decisão

### 1. Scope determina quais métodos o usuário pode chamar — não como os métodos se comportam internamente

| Método | Visibilidade | Scope que pode chamar |
|---|---|---|
| `getGlobalDashBoard(period)` | **public** | GLOBAL |
| `getConversionByStage(period, sector)` | **public** | GLOBAL + SECTOR |
| `getDropOffBySector(period)` | **public** | GLOBAL + SECTOR |
| `getUserPerformance(targetUserId, period)` | **public** | GLOBAL + OWN |
| `getAdsRoi(channel, period)` | **private** | — |
| `getPostProcedureMetrics(period)` | **private** | — |
| `getCalculatedBonus(targetId, periodRef)` | **private** | — |
| `resolveMetric(user, period)` | **private** | — |

### 2. Métodos que se tornam privados não têm permission check — são helpers puros

O orquestrador (`getGlobalDashBoard`) resolve o escopo uma única vez. Os helpers privados recebem dados como parâmetros e executam a lógica sem repetir o check.

### 3. Bonus — separação de responsabilidade entre ConfigService e AnalyticsService

- **BonusConfig (política)**: gerenciado por `ConfigService.getBonusConfigs(Sector sector)` — já existe. Expõe `bonusPct`, `targetValue`, `metricKey` por setor. É o que ADMs de setor precisam ver.
- **Bônus calculado por usuário**: helper interno `getCalculatedBonus`, chamado apenas por `getUserPerformance`. O resultado sobe embutido em `UserPerformanceResultDTO`.
- Não há gap: o ADM de setor lê a política via ConfigService; o usuário vê seu resultado via `getUserPerformance`.

---

## Implementação — passo a passo

### Passo 1 — `AnalyticsService` (interface)

Remover da interface:
- `AdsRoiResultDTO getAdsRoi(AdsChannel channel, DataRangeDTO period)`
- `BonusResultDTO getCalculatedBonus(UUID targetId, String periodRef)`
- `PostProcedureResultDTO getPostProcedureMetrics(DataRangeDTO period)`

Interface final:

```java
public interface AnalyticsService {
    StageConversionResultDTO getConversionByStage(DataRangeDTO period, Sector sector);
    List<SectorDropOffResultDTO> getDropOffBySector(DataRangeDTO period);
    UserPerformanceResultDTO getUserPerformance(UUID targetUserId, DataRangeDTO period);
    GlobalDashBoardResultDTO getGlobalDashBoard(DataRangeDTO period);
}
```

---

### Passo 2 — `AnalyticsController`

Remover os endpoints que ficam sem contrato público:
- `GET /ads-roi` → remover
- `GET /post-procedure` → remover
- `GET /bonus/{id}` → remover

Controller final mantém apenas:
- `GET /dashboard` → `getGlobalDashBoard`
- `GET /conversion` → `getConversionByStage`
- `GET /dropoff` → `getDropOffBySector`
- `GET /user-performance/{targetUserId}` → `getUserPerformance`

---

### Passo 3 — `AnalyticsServiceImpl`: tornar métodos privados

Alterar visibilidade para `private` e remover o `checkOrThrow` interno de:
- `getAdsRoi` → `private AdsRoiResultDTO getAdsRoi(AdsChannel channel, DataRangeDTO period)`
- `getPostProcedureMetrics` → `private PostProcedureResultDTO getPostProcedureMetrics(DataRangeDTO period)`
- `getCalculatedBonus` → `private BonusResultDTO getCalculatedBonus(UUID targetId, String periodRef)`

`resolveMetric` já é private — nenhuma mudança.

---

### Passo 4 — `getConversionByStage` (GLOBAL + SECTOR)

**Problema atual**: `checkOrThrow(null, null)` → 403 para SECTOR.

**O que muda**:

```java
// 1. Substituir checkOrThrow por getScope
PermissionScope scope = permissionService
        .getScope(user, ANALYTICS, READ)
        .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

// 2. Se SECTOR, ignorar o sector recebido e forçar o setor do usuário
//    (impede que um ADM_LEADS consulte dados do setor COMMERCIAL)
Sector effectiveSector = scope == PermissionScope.SECTOR
        ? user.getSector()
        : sector;

// 3. Usar effectiveSector no filtro (lógica existente não muda)
```

O filtro existente `if (sector != null) { tickets = tickets.stream().filter(...) }` funciona sem alteração — só precisa receber `effectiveSector` no lugar de `sector`.

---

### Passo 5 — `getDropOffBySector` (GLOBAL + SECTOR)

**Problema atual**: `checkOrThrow(null, null)` → 403 para SECTOR. Além disso, retorna dados dos 3 setores — um ADM_LEADS não deve ver dados do setor EVALUATOR.

**O que muda**:

```java
// 1. Substituir checkOrThrow por getScope
PermissionScope scope = permissionService
        .getScope(user, ANALYTICS, READ)
        .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
```

No retorno, após construir a lista com os 3 setores:

```java
// 2. Se SECTOR, filtrar para retornar apenas o setor do usuário
List<SectorDropOffResultDTO> result = List.of(
        buildDropOff(LEADS, leadsEntry, leadsLoss),
        buildDropOff(EVALUATOR, evaluatorEntry, evaluatorLoss),
        buildDropOff(COMMERCIAL, commercialEntry, commercialLoss)
);

return scope == PermissionScope.SECTOR
        ? result.stream().filter(r -> r.sector() == user.getSector()).toList()
        : result;
```

A lógica de cálculo das métricas não muda — apenas o que é retornado.

---

### Passo 6 — `getUserPerformance` (GLOBAL + OWN)

**Estado atual**: `checkOrThrow(user, ANALYTICS, READ, user.getSector(), user.getId())`.

- Para GLOBAL (ADM_SYSTEM): passa — GLOBAL sempre passa independente dos parâmetros.
- Para OWN (USER_*): passa quando `user.getId() == targetUserId` — ou seja, o usuário só vê a própria performance.
- Para SECTOR (ADM_LEADS etc.): passa — `user.getSector() == user.getSector()` é sempre verdadeiro.

**Problema**: ADM_LEADS pode chamar `getUserPerformance` para um usuário de outro setor. O check não valida o `targetUserId`, só o próprio usuário.

**O que muda**: substituir o check atual para usar `getScope()` e aplicar a guarda correta:

```java
PermissionScope scope = permissionService
        .getScope(user, ANALYTICS, READ)
        .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

// OWN: só pode consultar a si mesmo
if (scope == PermissionScope.OWN && !user.getId().equals(targetUserId)) {
    throw new AccessDeniedException("Acesso negado");
}
```

Para GLOBAL não há restrição adicional. SECTOR não acessa este endpoint (não está na tabela de acesso), então não precisa de guarda de setor aqui.

---

### Passo 7 — `getGlobalDashBoard` (GLOBAL only)

**Problema atual**: `checkOrThrow(null, null)` → 403 para SECTOR/OWN.

**O que muda**:

```java
// 1. Resolver escopo
PermissionScope scope = permissionService
        .getScope(user, ANALYTICS, READ)
        .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

// 2. Restringir ao escopo GLOBAL
if (scope != PermissionScope.GLOBAL) {
    throw new AccessDeniedException("Acesso restrito ao ADM_SYSTEM");
}

// 3. Substituir as chamadas internas por chamadas aos métodos privados
//    (sem novo checkOrThrow — escopo já resolvido acima)
```

As chamadas internas `getAdsRoi(channel, period)`, `getConversionByStage(period, null)`, `getDropOffBySector(period)` e `getUserPerformance(u.getId(), period)` continuam funcionando — agora chamam os helpers privados diretamente, sem disparar novos checks.

> **Atenção**: `getConversionByStage` e `getDropOffBySector` passam a ter lógica de scope internamente (Passos 4 e 5). Quando chamados por `getGlobalDashBoard` (que já validou GLOBAL), o `getScope()` interno vai retornar GLOBAL e o filtro de setor não será aplicado — comportamento correto.

---

## Ordem de execução recomendada

```
1. AnalyticsService.java         → remover 3 métodos da interface
2. AnalyticsController.java      → remover 3 endpoints
3. AnalyticsServiceImpl.java     → tornar 3 métodos private + remover checkOrThrow internos
4. getConversionByStage()        → getScope() + effectiveSector
5. getDropOffBySector()          → getScope() + filtro de retorno por sector
6. getUserPerformance()          → getScope() + guarda OWN
7. getGlobalDashBoard()          → getScope() + guarda GLOBAL
```

> Executar nessa ordem porque os passos 4–7 dependem dos métodos já serem private (passo 3) para não disparar checks em cascata.

---

## Consequências

### Positivas
- 403 eliminado para SECTOR e OWN nos endpoints corretos.
- Sem `checkOrThrow` em cascata: o orquestrador resolve o escopo uma vez.
- ADM de setor recebe apenas os dados do seu setor (sem vazamento entre setores).
- `BonusConfig` (política) permanece em `ConfigService` — responsabilidade correta.
- Métodos privados são helpers puros: testáveis unitariamente sem mock de `PermissionService`.

### Negativas / Riscos
- `/ads-roi`, `/post-procedure` e `/bonus/{id}` deixam de existir como endpoints independentes. Se o frontend já consome essas rotas, precisa ser atualizado.
- `getPostProcedureMetrics` torna-se privado mas `GlobalDashBoardResultDTO` ainda não o inclui. Os dados continuam acessíveis apenas via dashboard. Se for necessário expor separadamente no futuro, requer novo endpoint com escopo explícito.
- `getUserPerformance` chamado internamente por `getGlobalDashBoard` vai executar `getScope()` novamente para cada usuário da lista. Impacto negligenciável agora (regras em memória após seeder), mas deve ser revisto se cache de permissões for implementado (ver `spec-redis-cache.md`).

---

## Referências Cruzadas

- `ADR-012` — padrão RBAC list vs single (`getScope()` é a fonte do escopo)
- `ConfigService.getBonusConfigs(Sector)` — leitura de política de bônus por setor (já existe)
- `PermissionSeeder.java` — fonte de verdade da matriz ANALYTICS:READ por role
- `spec-redis-cache.md` — cache de `permissionRules` relevante para múltiplas chamadas a `getScope()` no loop de `getGlobalDashBoard`