# ADR-007: GET endpoints de configuração, RecycleConfig global e BonusResultDTO

**Status**: Aceito  
**Data**: 2026-06-01  
**Autores**: Arquiteto-Agent + Product Owner-Agent  
**Impacto**: Módulo commercial — ConfigController, ConfigService, ConfigServiceImpl, RecycleConfigRequestDTO; Módulo analytics — AnalyticsController, AnalyticsService, AnalyticsServiceImpl, BonusResultDTO

---

## Contexto

### Situação atual

`ConfigController` expõe apenas endpoints `POST` para as três configurações do sistema (`/config/recycle`, `/config/bonus`, `/config/ads-investment`). Não existe nenhum endpoint `GET` — é impossível consultar a configuração ativa sem acesso direto ao banco. Isso viola o princípio básico de API REST: se um recurso pode ser escrito, deve poder ser lido.

Adicionalmente, `GET /analytics/bonus/{id}` retorna `ResponseEntity<BigDecimal>` — um escalar primitivo na raiz da resposta. Isso viola ADR-001 (toda resposta de recurso deve ser envolvida em um DTO) e torna o contrato de API frágil: qualquer evolução futura (adicionar campos ao resultado de bônus) exigiria um breaking change.

Por fim, `RecycleConfig` foi modelado como configuração por setor (`sector` obrigatório no DTO). A análise de uso revelou que clínicas odontológicas operam com um único prazo de reciclagem global — a segmentação por setor adiciona complexidade sem valor de negócio real.

---

## Decisões

### 1. `BonusResultDTO` — wrapper para `GET /analytics/bonus/{id}`

`BigDecimal` retornado diretamente é substituído por um record:

```
BonusResultDTO(BigDecimal value)
```

Arquivos alterados: `AnalyticsService`, `AnalyticsServiceImpl`, `AnalyticsController`.

---

### 2. RecycleConfig passa a ser global

O campo `sector` é removido de `RecycleConfigRequestDTO`. O service deixa de usar setor para deativar configs anteriores e passa a deativar qualquer config ativa existente, independentemente de setor.

**O campo `sector` permanece na tabela `recycle_config`** — não há migration. Registros existentes com `sector` preenchido coexistem sem impacto: o código simplesmente para de ler e escrever esse campo. Valores históricos ficam órfãos no banco mas não causam inconsistência funcional.

**Query de leitura obrigatória:**

```
findFirstByActiveTrueOrderByCreatedAtDesc()
```

Sem `OrderByCreatedAtDesc`, se existirem múltiplos registros `active = true` herdados do modelo anterior (por setor), o PostgreSQL não garante qual será retornado. A ordenação torna o resultado determinístico.

**Permissão:** `checkOrThrow` em `setRecycleConfig()` passa `sector = null` — consistente com `registerAdsInvestment()` que já usa o mesmo padrão (`CONFIG + CONFIGURE + null`).

---

### 3. GET endpoints de configuração

Três novos endpoints adicionados ao `ConfigController`:

| Endpoint | Query param | Retorno |
|---|---|---|
| `GET /config/recycle` | nenhum | `RecycleConfigResponseDTO` |
| `GET /config/bonus` | `?sector=LEADS` (obrigatório) | `List<BonusConfigResponseDTO>` |
| `GET /config/ads-investment` | `?channel=INSTAGRAM` (obrigatório) | `List<AdsInvestmentResponseDTO>` |

**Por que `bonus` retorna List?** Um setor pode ter múltiplas configs de bônus ativas (por `role` diferente — `USER_COMMERCIAL` e `ADM_COMMERCIAL` têm regras distintas). Retornar apenas um seria perda de informação.

**Por que `ads-investment` retorna List?** `AdsInvestment` é um registro histórico de aportes, não uma configuração singleton. Vários registros podem existir para o mesmo canal em períodos diferentes.

---

### 4. Response DTOs para os 3 GETs

Três novos records criados no pacote `commercial/api/dto/response/`:

**`RecycleConfigResponseDTO`**

| Campo | Tipo | Nota |
|---|---|---|
| `id` | UUID | |
| `afterDays` | int | |
| `active` | boolean | |
| `createdAt` | LocalDateTime | |

`sector` deliberadamente omitido — config agora é global.

---

**`BonusConfigResponseDTO`**

| Campo | Tipo |
|---|---|
| `id` | UUID |
| `sector` | Sector |
| `role` | Role |
| `metricKey` | String |
| `bonusPct` | BigDecimal |
| `targetValue` | BigDecimal |
| `periodRef` | String |
| `active` | boolean |
| `createdAt` | LocalDateTime |

---

**`AdsInvestmentResponseDTO`**

| Campo | Tipo | Nota |
|---|---|---|
| `id` | UUID | |
| `channel` | AdsChannel | |
| `campaign` | String | |
| `amount` | BigDecimal | |
| `periodStart` | LocalDate | |
| `periodEnd` | LocalDate | |
| `createdAt` | LocalDateTime | |

`registeredBy` deliberadamente omitido — dado interno de auditoria, não relevante para o consumidor da API.

---

## Mapa de arquivos

```
NOVOS (4 arquivos)
├── analytics/api/dto/BonusResultDTO.java
├── commercial/api/dto/response/RecycleConfigResponseDTO.java
├── commercial/api/dto/response/BonusConfigResponseDTO.java
└── commercial/api/dto/response/AdsInvestmentResponseDTO.java

MODIFICADOS (7 arquivos)
├── analytics/service/AnalyticsService.java                  (assinatura getCalculatedBonus)
├── analytics/service/impl/AnalyticsServiceImpl.java         (retorno BonusResultDTO)
├── analytics/api/controller/AnalyticsController.java        (ResponseEntity<BonusResultDTO>)
├── commercial/api/dto/request/recycleConfig/RecycleConfigRequestDTO.java  (remove sector)
├── commercial/service/ConfigService.java                    (+ 3 métodos GET)
├── commercial/service/impl/ConfigServiceImpl.java           (+ 3 implementações, ajuste POST)
└── commercial/api/controller/ConfigController.java          (+ 3 @GetMapping)

BANCO DE DADOS
└── Nenhuma migration necessária
```

---

## Consequências Positivas

- `GET /analytics/bonus/{id}` passa a ter contrato evoluível sem breaking change
- Configuração ativa pode ser lida via API — elimina necessidade de acesso direto ao banco para operações de suporte
- RecycleConfig global reduz superfície de configuração da clínica: um campo a ajustar em vez de N (um por setor)
- Response DTOs desacoplam o modelo interno do contrato público — mudanças na entidade não quebram a API

## Consequências Negativas / Riscos

- `sector` na tabela `recycle_config` fica como coluna órfã. Risco: baixo. Mitigação: migration de cleanup pode ser feita em fase futura sem impacto funcional.
- `findFirstByActiveTrueOrderByCreatedAtDesc()` pode retornar config desatualizada se a desativação falhar em alguma operação anterior. Risco: baixo, coberto por `@Transactional` em `setRecycleConfig()`.
- `GET /config/bonus` e `GET /config/ads-investment` não têm paginação. Risco aceitável no horizonte do MVP — volume de configurações de uma clínica é intrinsecamente pequeno.

---

## Alternativas Consideradas

- **Retornar entidade diretamente nos GETs**: descartado. Expor o modelo de domínio como contrato de API é o principal vetor de breaking changes em APIs que evoluem.
- **RecycleConfig por setor**: descartado. Não existe caso de uso real de prazos de reciclagem diferentes por setor numa clínica odontológica de pequeno/médio porte.
- **`GET /config/bonus` sem filtro (retorna tudo)**: descartado. Sem filtro por setor, a resposta mistura regras de roles diferentes de setores diferentes — inútil para o consumidor que precisa saber "qual é a regra do meu setor".

---

## Referências Cruzadas

- `ADR-001` — query params para filtros, path params para ID único; aplicado em `?sector` e `?channel`
- `ADR-004` — `checkOrThrow` com `sector = null` para ações globais; aplicado em `setRecycleConfig()` após remoção do setor do DTO
- `RecycleConfigRepository` — método `findBySectorAndActiveTrue()` existente pode ser mantido para retrocompatibilidade interna; adicionar `findFirstByActiveTrueOrderByCreatedAtDesc()` para o novo GET