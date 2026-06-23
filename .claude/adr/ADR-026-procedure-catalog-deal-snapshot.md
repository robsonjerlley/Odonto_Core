# ADR-026: Catálogo de Procedimentos (`Procedure`) + Snapshot em `DealProcedure`

**Status**: Proposto  
**Data**: 2026-06-23  
**Autores**: Arquiteto-Agent  
**Impacto**: módulo `commercial` (Deal, DealProcedure), novo módulo `catalog` (Procedure), `DealCreateRequestDTO`, `DealUpdateRequestDTO`, `DealServiceImpl`, CLAUDE.md  
**Relaciona**: ADR-023 (TicketWonEvent — módulo de agendamento depende de `Procedure.estimatedDuration`), ADR-024 (`@TenantId` — `Procedure` é multi-tenant por `clinicId`)

---

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
2. **Bloqueio para agendamento**: o módulo de agendamento (ADR-023) precisará agendar por *tipo de procedimento* com duração estimada (`estimatedDuration`). Sem um catálogo, não há referência canônica para criar slots de agenda.
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

```
create(ticketId, dto):
  1. Carregar todos os Procedure por ID em batch (único SELECT IN)
  2. Validar: todos existem, todos active = true, todos pertencem ao tenant atual
  3. Para cada DealItemRequestDTO:
       snapshot = new DealProcedure(
           procedureId   = catalog.id,
           name          = catalog.name,          // snapshot
           code          = catalog.code,          // snapshot
           tableValue    = catalog.defaultPrice,  // snapshot
           priceOverride = item.priceOverride,    // nullable
           quantity      = item.quantity,
           note          = item.note
       )
  4. totalValue = Σ (priceOverride ?? tableValue) × quantity
  5. Salvar Deal com snapshot JSONB
```

A validação de `active = true` impede que procedimentos desativados sejam incluídos em novos deals, sem impactar deals históricos que já carregam o snapshot.

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
  scheduling/                     ← também dependerá de catalog (futuro, ADR-023)
```

Dependência unidirecional: `commercial` → `catalog` ← `scheduling`. O catálogo não conhece nenhum dos dois.

---

## Arquivos atingidos

### Novos

| Arquivo | Responsabilidade |
|---|---|
| `modules/catalog/domain/model/Procedure.java` | Entidade catálogo com `@TenantId` |
| `modules/catalog/repository/ProcedureRepository.java` | JPA + Specifications (busca por nome/código) |
| `modules/catalog/service/ProcedureService.java` | Interface pública |
| `modules/catalog/service/impl/ProcedureServiceImpl.java` | Implementação com validação de soft delete |
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
| `modules/commercial/service/impl/DealServiceImpl.java` | Carrega catálogo antes de construir snapshot; novo cálculo de `totalValue` com `priceOverride` |
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
2. Procedure.java + ProcedureRepository.java     → entidade com @TenantId
3. ProcedureService + ProcedureServiceImpl       → CRUD com soft delete
4. ProcedureController                           → endpoints REST
5. DealProcedure.java                            → adicionar procedureId + priceOverride
6. DealItemRequestDTO                            → substituir DealProcedureDTO
7. DealServiceImpl.create() + update()           → carregar catálogo + construir snapshot
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

- ADR-023 — TicketWonEvent (agendamento precisará de `Procedure.estimatedDuration`)
- ADR-024 — `@TenantId` enforcement (`Procedure` nasce com `@TenantId`)
- ADR-001 — API search/lookup pattern (aplicado ao `ProcedureController`)
- ADR-002 — Interface vs Impl (aplicado a `ProcedureService`)
- ADR-014 — Flyway (migration `V[n]__create_procedures.sql`)
