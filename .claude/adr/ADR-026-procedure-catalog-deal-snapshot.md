# ADR-026: Catálogo de Procedimentos (`Procedure`) + Snapshot em `DealProcedure`

**Status**: Implementado — 2026-06-27 (fechado de fato em 2026-07-01)  
**Data**: 2026-06-23  
**Autores**: Arquiteto-Agent  
**Impacto**: módulo `commercial` (Deal, DealProcedure), novo módulo `catalog` (Procedure), `DealCreateRequestDTO`, `DealUpdateRequestDTO`, `DealServiceImpl`, CLAUDE.md  
**Relaciona**: ADR-029 (módulo `appointment` — consome `Procedure.estimatedDuration` via `ProcedureProvider`; substitui a referência original à ADR-023), ADR-024 (`@TenantId` — `Procedure` é multi-tenant por `clinicId`)

---

> ✅ **Fechamento 2026-07-01 — pendências concluídas.** A ADR estava marcada "Implementado" mas dois itens do contrato não haviam sido entregues: (1) o **response enriquecido** — `DealResponseDTO.items` retornava `List<DealItemRequestDTO>` (o DTO de *request*), agora corrigido para `List<DealProcedureResponseDTO>` com `effectivePrice`/`subtotal` calculados no `DealMapper.toProcedureResponse`; (2) o **RBAC do catálogo** — não havia nenhuma regra `PROCEDURE` no `PermissionSeeder` (o `ProcedureServiceImpl` já chamava `checkOrThrow`, mas sem regra tudo dava `AccessDenied`). Seedado: write=`ADM_SYSTEM` (GLOBAL), read=todos os papéis (GLOBAL). Nota de processo: a ADR não deveria ter sido marcada "Implementado" com o contrato incompleto.

> ⚠️ **Revisão 2026-06-28 — `estimatedDuration` removido.** O campo `estimated_duration` (entity `Procedure`, `ProcedureView`, DTOs do catálogo) foi **removido**: o módulo `appointment` — seu único consumidor — deixou de usar duração (agendamento por data/hora basta; ver ADR-029). As menções a `estimatedDuration`/`estimated_duration`/slots de agenda abaixo são **históricas**.

## Contexto

`DealProcedure` é hoje um `record` sem persistência própria, armazenado como JSONB dentro de `Deal`:

```java
public record DealProcedure(
    String name,       // digitado livre pelo avaliador
    String code,       // digitado livre
    BigDecimal tableValue,
    int quantity,
    String note
)
```

O avaliador digita nome e valor de cada procedimento manualmente no momento de criar o Deal. Isso gera três problemas concretos:

1. **Inconsistência de dados**: "Clareamento", "clareamento dental" e "CLAREAMENTO A LASER" são tratados como procedimentos distintos em relatórios de receita, impossibilitando agrupamento confiável por tipo.
2. **Bloqueio para agendamento**: o módulo `appointment` (ADR-029) precisará agendar por *tipo de procedimento* com duração estimada (`estimatedDuration`). Sem um catálogo, não há referência canônica para criar slots de agenda.
3. **Sem referência histórica rastreável**: não é possível responder "qual procedimento X foi realizado em quantos pacientes?" — o texto livre não é indexável de forma confiável.

---

## Decisão

**Criar entidade `Procedure` como catálogo persistido por clínica** (multi-tenant via `@TenantId`), e modificar `DealProcedure` para referenciar o catálogo enquanto mantém um snapshot imutável dos valores no momento do deal.

O design de armazenar `procedures` como **JSONB snapshot em `Deal` é mantido** — é correto, pois preserva o valor negociado historicamente, independente de alterações futuras no catálogo (renomeação de procedimento, reajuste de preço). O que muda é que o snapshot agora **parte do catálogo**, não de texto livre.

---

## Análise de Trade-offs

| Critério | Value Object livre (atual) | Catálogo `Procedure` (esta ADR) | Peso |
|---|---|---|---|
| Consistência de dados | ❌ Texto livre = sem garantia | ✅ Canônico por clínica | Alto |
| Integração com agendamento | ❌ Inviável sem retrabalho | ✅ `procedureId` + `estimatedDuration` | Alto |
| Relatório por procedimento | ❌ Texto livre ≠ agrupável | ✅ Agrupamento por UUID confiável | Alto |
| Integridade histórica de Deal | ✅ Snapshot JSONB | ✅ Snapshot JSONB + `procedureId` | Alto |
| Complexidade de implementação | ✅ Baixa | Média (nova entidade + tela de cadastro) | Médio |
| Time-to-market agora | ✅ Zero | Uma tela a mais de onboarding | Médio |
| Evolução futura | ⚠️ Quebra de contrato garantida | ✅ Extensível | Alto |

🎯 **O custo de adicionar o catálogo agora é uma tela de cadastro. O custo de não adicionar é uma migração de dados textuais inconsistentes com o agendamento já integrado.**

---

## Contrato das entidades

### 1. Entidade `Procedure` — catálogo por clínica

```
Tabela: crm_db.procedures
─────────────────────────────────────────────────────────
id                : UUID           PK
clinic_id         : UUID           NOT NULL — @TenantId (ADR-024)
code              : VARCHAR        nullable — código CBHPM/TUSS ou interno da clínica
name              : VARCHAR        NOT NULL
default_price     : NUMERIC(15,2)  NOT NULL — valor de tabela padrão
estimated_duration: INTEGER        nullable — minutos (alimenta slots de agendamento)
active            : BOOLEAN        NOT NULL DEFAULT true
created_by        : UUID           NOT NULL
created_at        : TIMESTAMP      NOT NULL
updated_at        : TIMESTAMP      NOT NULL

Constraints:
  UNIQUE (clinic_id, code) WHERE code IS NOT NULL
  CHECK  (estimated_duration > 0)
```

**Soft delete via `active = false`** — nunca apagar `Procedure` referenciada em Deal (o `procedureId` no snapshot JSONB continuaria apontando para registro inexistente).

### 2. `DealProcedure` — record modificado (snapshot enriquecido)

```java
public record DealProcedure(
    UUID procedureId,          // FK lógica ao catálogo — rastreabilidade
    String name,               // snapshot do nome no momento do deal
    String code,               // snapshot do código
    BigDecimal tableValue,     // snapshot do defaultPrice no momento do deal
    BigDecimal priceOverride,  // nullable — preço negociado neste deal específico
    int quantity,
    String note                // nullable
)
```

**Cálculo do `totalValue` em `Deal`:**
```
valorEfetivo(item) = priceOverride ?? tableValue
totalValue = Σ valorEfetivo(item) × item.quantity
```

O `priceOverride` permite que o avaliador negocie um valor diferente do padrão do catálogo sem perder a referência ao preço de tabela original — relevante para relatório de desconto médio por procedimento.

---

## Contrato de API

### Catálogo — `ProcedureController`

| Método | Path | Acesso mínimo | Descrição |
|--------|------|---------------|-----------|
| `POST` | `/api/procedures` | MANAGER / ADM | Cria procedure no catálogo da clínica |
| `GET` | `/api/procedures` | Todos autenticados | Lista ativos, paginado; aceita `?name=` e `?code=` |
| `GET` | `/api/procedures/{id}` | Todos autenticados | Detalhe por ID |
| `PUT` | `/api/procedures/{id}` | MANAGER / ADM | Atualiza nome, preço, duração |
| `DELETE` | `/api/procedures/{id}` | MANAGER / ADM | Soft delete (`active = false`) |

> Padrão ADR-001: `GET /api/procedures/{id}` retorna único ou 404. `GET /api/procedures?name=X` retorna lista, pode ser vazia.

### Catálogo — `ProcedureService` (API interna do módulo, consumida por `commercial` / `appointment`)

Além do CRUD, o `catalog` expõe um método para consumidores que precisam **operar sobre um conjunto** de procedimentos. É por aqui que `commercial` entra — nunca pelo `ProcedureRepository`.

```java
/**
 * Resolve os procedimentos ATIVOS do tenant atual para os IDs informados.
 * Pré-condição : ids não vazio.
 * Pós-condição : retorna exatamente um Procedure por id, todos active = true.
 * Fail-fast    : ResourceNotFoundException se algum id não existir (ou for de
 *                outro tenant — o @TenantId já o exclui do SELECT);
 *                IllegalStateException se algum existir porém inativo.
 */
List<Procedure> resolveActiveByIds(List<UUID> ids);
```

> **Nome domínio-neutro de propósito.** O `catalog` não conhece `Deal`. Nomear o método `getForDeal(...)` vazaria o domínio do `commercial` para dentro do `catalog` — acoplamento na direção errada. `appointment` (ADR-029) reusa o mesmo `resolveActiveByIds` para montar slots de agenda.

> ⚠️ **Revisado pela ADR-028 (2026-06-24).** Este método foi movido para uma interface dedicada `ProcedureProvider` (ISP — ADR-002) e seu retorno mudou de `List<Procedure>` (entidade JPA) para `List<ProcedureView>` (read-model imutável), evitando o vazamento da persistência do `catalog` para o `commercial`. A mesma ADR-028 troca `findByName` por um `search(name, code, Pageable)` paginado (ADR-001). Ver ADR-028 para o contrato vigente.

### DTOs modificados

**`DealItemRequestDTO`** — substitui `DealProcedureDTO`:
```java
public record DealItemRequestDTO(
    @NotNull UUID procedureId,
    BigDecimal priceOverride,   // nullable — ausente = usa defaultPrice do catálogo
    @Min(1) int quantity,
    String note                 // nullable
)
```

**`DealCreateRequestDTO`** — campo renomeado:
```java
public record DealCreateRequestDTO(
    @NotEmpty List<DealItemRequestDTO> items
)
```

**`DealUpdateRequestDTO`** — idem.

**`DealProcedureResponseDTO`** — no response do Deal:
```java
public record DealProcedureResponseDTO(
    UUID procedureId,
    String name,
    String code,
    BigDecimal tableValue,
    BigDecimal priceOverride,    // nullable
    int quantity,
    BigDecimal effectivePrice,   // calculado: priceOverride ?? tableValue
    BigDecimal subtotal,         // calculado: effectivePrice × quantity
    String note
)
```

---

## Lógica modificada em `DealServiceImpl`

> 📐 **Decisão de fronteira de módulo (revisão 2026-06-23):** `commercial` **não** injeta `ProcedureRepository`. A busca em batch e a validação (existe / `active` / tenant) são responsabilidade do módulo `catalog`, expostas pela sua interface pública `ProcedureService`. O `DealServiceImpl` depende da abstração do catálogo, não da sua persistência (Dependency Inversion — ADR-002). Assim, a regra "o que é um `Procedure` válido" mora onde o `Procedure` mora: se ela evoluir (ex.: validade por data), só o `catalog` muda. A versão anterior desta ADR colocava carga + validação dentro do `create()` — isso violava Single Responsibility e vazava regra do `catalog` para o `commercial`.

O `create()` mantém **uma responsabilidade**: orquestrar o ciclo de vida do `Deal`. A carga + validação do catálogo é delegada; a montagem do snapshot e o cálculo do `totalValue` permanecem no `commercial`, porque são conceitos do *Deal* (`priceOverride`, `tableValue` snapshot, `totalValue`) que o `catalog` desconhece.

```
create(ticketId, dto):
  1. autorizar (PermissionService) + validar ticket (IN_EVALUATION → NEGOTIATION)
  2. procedures = procedureService.resolveActiveByIds(ids)    ← delega ao catalog (fail-fast)
  3. snapshot   = buildSnapshot(dto.items(), procedures)      ← privado, no commercial
  4. totalValue = calcTotal(snapshot)                         ← privado, no commercial
  5. salvar Deal com snapshot JSONB

buildSnapshot(items, procedures):
  byId = procedures indexados por id
  para cada item:
      catalog = byId[item.procedureId]
      new DealProcedure(
          procedureId   = catalog.id,
          name          = catalog.name,          // snapshot
          code          = catalog.code,          // snapshot
          tableValue    = catalog.defaultPrice,  // snapshot do preço de tabela
          priceOverride = item.priceOverride,    // nullable — valor negociado
          quantity      = item.quantity,
          note          = item.note
      )

calcTotal(snapshot):
  Σ (priceOverride ?? tableValue) × quantity
```

`buildSnapshot` e `calcTotal` ficam em métodos privados do `DealServiceImpl` (ou como métodos de domínio em `Deal`/`DealProcedure`). O `Deal` é montado no `commercial`, não fora dele.

`update()` segue a mesma estrutura: delega `resolveActiveByIds` ao `catalog`, reconstrói snapshot + `totalValue` pelos mesmos privados e registra `DealHistory`.

A validação de `active = true` (feita no `catalog`) impede que procedimentos desativados entrem em novos deals, sem impactar deals históricos que já carregam o snapshot.

---

## Localização do módulo

`Procedure` **não entra no módulo `commercial`**. Quando o módulo de agendamento chegar, ele precisará do catálogo de procedimentos (para saber duração estimada de cada slot). Se o catálogo estiver em `commercial`, o agendamento passaria a depender de `commercial` — coupling incorreto.

```
modules/
  catalog/                        ← novo módulo independente
    domain/model/Procedure.java
    repository/ProcedureRepository.java
    service/ProcedureService.java
    service/impl/ProcedureServiceImpl.java
    api/controller/ProcedureController.java
    api/dto/request/ProcedureCreateRequestDTO.java
    api/dto/request/ProcedureUpdateRequestDTO.java
    api/dto/response/ProcedureResponseDTO.java
  commercial/                     ← depende de catalog (lê Procedure por ID)
  appointment/                     ← também dependerá de catalog (futuro, ADR-023)
```

Dependência unidirecional: `commercial` → `catalog` ← `appointment`. O catálogo não conhece nenhum dos dois.

**Regra de acoplamento entre módulos:** a dependência atravessa apenas a **interface pública** `ProcedureService`. Nenhum módulo externo injeta `ProcedureRepository` — a persistência do `catalog` é detalhe interno. Isso preserva Dependency Inversion (ADR-002) e mantém as regras de invariante do `Procedure` (o que é "ativo", como buscar) dentro do próprio módulo.

---

## Arquivos atingidos

### Novos

| Arquivo | Responsabilidade |
|---|---|
| `modules/catalog/domain/model/Procedure.java` | Entidade catálogo com `@TenantId` |
| `modules/catalog/repository/ProcedureRepository.java` | JPA + `findAllById` (batch) + Specifications (busca por nome/código) |
| `modules/catalog/service/ProcedureService.java` | Interface pública: CRUD + `resolveActiveByIds` (consumo cross-módulo) |
| `modules/catalog/service/impl/ProcedureServiceImpl.java` | Implementação com soft delete e validação fail-fast de `resolveActiveByIds` |
| `modules/catalog/api/controller/ProcedureController.java` | CRUD REST |
| `modules/catalog/api/dto/request/ProcedureCreateRequestDTO.java` | |
| `modules/catalog/api/dto/request/ProcedureUpdateRequestDTO.java` | |
| `modules/catalog/api/dto/response/ProcedureResponseDTO.java` | |
| Migration Flyway `V[n]__create_procedures.sql` | `CREATE TABLE crm_db.procedures` |

### Modificados

| Arquivo | Mudança |
|---|---|
| `modules/commercial/model/DealProcedure.java` | Adiciona `procedureId`, `priceOverride`; remove campos de texto livre como entrada |
| `shared/DealProcedureDTO.java` | Substitui por `DealItemRequestDTO` (campos: `procedureId`, `priceOverride`, `quantity`, `note`) |
| `modules/commercial/api/dto/request/deal/DealCreateRequestDTO.java` | `procedures` → `items` do tipo `DealItemRequestDTO` |
| `modules/commercial/api/dto/request/deal/DealUpdateRequestDTO.java` | idem |
| `modules/commercial/service/impl/DealServiceImpl.java` | Injeta `ProcedureService` (não `ProcedureRepository`); delega validação a `resolveActiveByIds`; `buildSnapshot` + cálculo de `totalValue` (com `priceOverride`) em métodos privados |
| `modules/commercial/mapper/DealMapper.java` | Mapear snapshot para `DealProcedureResponseDTO` com campos calculados |
| `CLAUDE.md` | Atualizar descrição de `DealProcedure`, adicionar módulo `catalog` na estrutura, atualizar índice ADRs |

### Não modificados

| Arquivo | Motivo |
|---|---|
| `Deal.java` — campo `procedures` | Continua `List<DealProcedure>` em JSONB; apenas o Record muda |
| `DealHistory.java` | Imutável — continua gravando `valueBefore`/`valueAfter` em JSON como está |
| `DealHistoryService` | Sem mudança de contrato |
| `AnalyticsServiceImpl` | Passa a poder agregar receita por `procedureId` via JSONB path — evolução futura, não bloqueante |

---

## Ordem de implementação

```
1. Migration V[n]__create_procedures.sql         → CREATE TABLE crm_db.procedures
2. Procedure.java + ProcedureRepository.java     → entidade com @TenantId + findAllById
3. ProcedureService + ProcedureServiceImpl       → CRUD com soft delete + resolveActiveByIds (fail-fast)
4. ProcedureController                           → endpoints REST
5. DealProcedure.java                            → adicionar procedureId + priceOverride
6. DealItemRequestDTO                            → substituir DealProcedureDTO
7. DealServiceImpl.create() + update()           → injetar ProcedureService; delegar resolveActiveByIds + buildSnapshot privado
8. DealMapper                                    → mapear para DealProcedureResponseDTO
9. CLAUDE.md                                     → sincronizar contexto
```

---

## Consequências positivas

- Agendamento referencia `Procedure.id` diretamente — sem retrabalho quando o módulo chegar.
- Relatórios de receita por procedimento são confiáveis a partir do primeiro deal com catálogo.
- `estimatedDuration` permite calcular slots de agenda de forma automática.
- `priceOverride` registra o desconto por procedimento (granularidade maior que o desconto percentual global do Deal).
- Integridade histórica mantida — snapshot JSONB preserva valores no momento da negociação.
- `active = false` (soft delete) protege o histórico sem apagar o procedimento referenciado.

## Consequências negativas / riscos

| Risco | Mitigação |
|---|---|
| Onboarding: clínica precisa cadastrar catálogo antes do primeiro Deal | Seeder de procedimentos odontológicos comuns no primeiro boot (futuro); por ora, tela de cadastro simples |
| `DealProcedureDTO` público quebra clientes da API se houver integrações | Nenhuma integração externa hoje — mudança segura neste momento |
| Analytics por `procedureId` via JSONB path requer query específica | Adiar para quando analytics de procedimento for solicitado; não é pré-requisito desta ADR |
| `Procedure` desativado depois de usado em Deals históricos | O snapshot JSONB preserva nome/valor; `procedureId` aponta para registro `active=false` — aceitável, basta não expor 404 para consultas históricas |

---

## Alternativas descartadas

- **Enum fixo de procedimentos**: descartado — cada clínica tem procedimentos e valores distintos. Um enum global impossibilita personalização por tenant.
- **Manter value object com validação por lista fixa (whitelist)**: descartado — transfere o problema de manutenção para o código, não para o dado. Whitelist em código não é multi-tenant.
- **Tabela relacional `deal_procedures` com FK para `deals`**: considerada. Vantagem: queries relacionais diretas. Descartada porque o JSONB já existe, funciona, é performático para o volume esperado, e a imutabilidade histórica do snapshot é uma propriedade desejável que tabela relacional exigiria colunas de "snapshot" de qualquer forma.

---

## Referências

- ADR-029 — módulo `appointment` (consome `Procedure.estimatedDuration` via `ProcedureProvider`)
- ADR-024 — `@TenantId` enforcement (`Procedure` nasce com `@TenantId`)
- ADR-001 — API search/lookup pattern (aplicado ao `ProcedureController`)
- ADR-002 — Interface vs Impl (aplicado a `ProcedureService`)
- ADR-014 — Flyway (migration `V[n]__create_procedures.sql`)
