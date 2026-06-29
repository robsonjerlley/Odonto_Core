# ADR-028: Fronteira de leitura do `catalog` — `ProcedureProvider` (read-model) + `search()` unificado

**Status**: Implementado — 2026-06-27
**Data**: 2026-06-24
**Autores**: Arquiteto-Agent
**Impacto**: módulo `catalog` (`ProcedureService`, novo `ProcedureProvider`, novo `ProcedureView`), `commercial` (`DealServiceImpl`), `appointment` (ADR-029)
**Revisa**: ADR-026 (substitui a prescrição `List<Procedure> resolveActiveByIds`); aplica ADR-002 (ISP), ADR-001 (search/lookup pattern), ADR-013 (Specifications)

---

> ⚠️ **Revisão 2026-06-28:** `estimatedDuration` foi **removido** do `ProcedureView` (e do catálogo) — o `appointment` não consome mais duração (ADR-029). As menções a `estimatedDuration` abaixo são históricas; o `ProcedureView` vigente é `(id, name, code, defaultPrice)`.

## Contexto

A ADR-026 estruturou o catálogo (`Procedure`) e a fronteira `commercial → catalog`, mas deixou dois pontos do contrato de **leitura** imprecisos. A implementação evidenciou os dois problemas:

1. **Ponto C — método cross-módulo.** A ADR-026 prescreveu `List<Procedure> resolveActiveByIds(List<UUID>)` na mesma interface `ProcedureService` que carrega o CRUD. Devolver a **entidade JPA** `Procedure` contradiz o próprio princípio da ADR-026 — que proíbe `commercial` de injetar `ProcedureRepository` justamente para não acoplar à persistência do `catalog`. Entregar a entidade gerenciada é o mesmo vazamento por outra porta: `commercial` passaria a enxergar associações lazy, poderia mutar a entidade e disparar dirty-checking dentro da transação, e quebraria em compilação a cada evolução do schema do `Procedure`. No código, o stub ficou como `ProcedureResponseDTO isActive(List<UUID>)` retornando `null`, e o `DealServiceImpl` (linhas 109-111) passou a validar `active` **ele mesmo** (`!isActive.contains(true)`, lógica invertida) — exatamente a regra do `catalog` vazada para o `commercial`.

2. **`findByName` — contrato de identificador onde deveria ser filtro.** A assinatura ficou `ProcedureResponseDTO findByName(String)` sobre `Optional<Procedure> findByName` (match exato, `orElseThrow` → 404). Pela ADR-001, `name` é **filtro** (resultado pode ser lista vazia), não identificador único. Match exato ainda inviabiliza o caso de uso real: buscar "implante" e ver os candidatos ("Impladetal", "Ortodetal") para seleção por id.

---

## Decisão

### Parte 1 — Ponto C: read-model + provider port dedicado

**(A) Duas interfaces, uma implementação.** O CRUD e a resolução cross-módulo têm consumidores e eixos de mudança distintos:

```
catalog/
  service/ProcedureService.java      → contrato do controller (CRUD + search)
  provider/ProcedureProvider.java    → contrato cross-módulo (1 método)
  provider/ProcedureView.java        → read-model imutável
  service/impl/ProcedureServiceImpl  implements ProcedureService, ProcedureProvider
```

```java
public interface ProcedureProvider {
    /**
     * Resolve os procedimentos ATIVOS do tenant atual para os IDs informados.
     * Pré-condição : ids não vazio.
     * Pós-condição : retorna exatamente um ProcedureView por id, todos ativos.
     * Fail-fast    : ResourceNotFoundException se algum id não existir (ou for de
     *                outro tenant — @TenantId já o exclui do SELECT);
     *                IllegalStateException se algum existir porém inativo.
     */
    List<ProcedureView> resolveActiveByIds(List<UUID> ids);
}
```

**(B) Retorno é read-model, não entidade.**

```java
public record ProcedureView(
    UUID id,
    String name,
    String code,
    BigDecimal defaultPrice,    // vira tableValue no snapshot do Deal
    Integer estimatedDuration   // consumido pelo appointment (ADR-029)
) {}
```

- `ProcedureView` cobre `commercial` (id, name, code, defaultPrice) **e** `appointment` (id, estimatedDuration) num único tipo.
- **`active` é omitido de propósito.** O contrato já garante por fail-fast que todo `ProcedureView` retornado é ativo. Omitir o campo faz o *tipo* comunicar a invariante e remove qualquer `active` para o `commercial` checar — é o que mata a validação invertida das linhas 109-111 do `DealServiceImpl`.
- **Nome:** `View` (não `Snapshot` — reservado ao `DealProcedure` histórico; nem `...DTO` — sufixo da superfície REST). Três superfícies distintas e nomeadas: `ProcedureResponseDTO` (REST), `ProcedureView` (cross-módulo), `DealProcedure` (snapshot congelado).

### Parte 2 — `search()` unificado no lugar de `findByName`

```java
// interface ProcedureService — uma busca pública
Page<ProcedureResponseDTO> search(String name, String code, Pageable pageable);
```

- **Padrão 2 da ADR-001:** `name`/`code` são filtros → `GET /api/procedures?name=&code=` → `Page` (pode ser vazia, HTTP 200). **Nunca 404** — remove o `orElseThrow(ResourceNotFoundException)`.
- **`Page`**, porque a ADR-026 prescreve listagem paginada.
- **Match parcial** (`name ILIKE %x%`) via **Specification (ADR-013)**, combinando `name` + `code` + `active = true` sem cadeia `if/else`. Remove `Optional<Procedure> findByName` (match exato) do repositório.
- **Filtra `active = true`** — o avaliador só vê o que pode selecionar.
- **`findByName`/`findByCode` viram `private`** na impl (ADR-002: só `search` tem consumidor externo).
- **Corrige a autorização inconsistente:** hoje `findByName` passa `user.getSector(), user.getId()` enquanto `create/update/delete` passam `null, null`. GET é "todos autenticados" (ADR-026) → os args de escopo do `checkOrThrow` seguem o padrão de leitura, não o de escrita.

### Dois caminhos de leitura, deliberadamente separados

| Caminho | Quem chama | Retorna | Interface | Propósito |
|---|---|---|---|---|
| `search(name, code, pageable)` | `ProcedureController` (humano escolhe) | `Page<ProcedureResponseDTO>` | `ProcedureService` | mostrar candidatos ao avaliador |
| `resolveActiveByIds(ids)` | `commercial` / `appointment` (máquina resolve) | `List<ProcedureView>` | `ProcedureProvider` | montar o snapshot do Deal / slots de agenda |

`search` **alimenta** `resolveActiveByIds` (o humano busca por texto → seleciona ids → a máquina resolve), mas são superfícies, tipos e interfaces distintos.

---

## Análise de Trade-offs

### (A) Uma interface vs. duas

| Critério | 1 interface (CRUD + resolve) | 2 interfaces (CRUD + provider) | Peso |
|---|---|---|---|
| Aderência ISP | ⚠️ `commercial` depende de 5 métodos, usa 1 | ✅ depende de exatamente 1 | Alto |
| Superfície de mock no teste do `commercial` | ⚠️ mocka CRUD inteiro | ✅ mocka 1 método | Alto |
| Explicitar a fronteira da ADR-026 | ⚠️ fronteira implícita | ✅ fronteira é um TIPO defendido pelo compilador | Alto |
| Complexidade | ✅ um arquivo | ⚠️ um arquivo a mais (mesma impl) | Médio |
| Over-engineering | — | ⚠️ só seria risco se o 2º consumidor fosse especulativo | Médio |

O risco de over-engineering **não se aplica**: diferente do `CustomerService` da ADR-002 (cujos métodos não tinham nenhum consumidor externo), aqui o segundo consumidor — `appointment` — já está nomeado na ADR-029. Dois consumidores reais = contrato real.

### (B) Entidade vs. read-model

| Critério | Entidade `Procedure` | Read-model `ProcedureView` | Peso |
|---|---|---|---|
| Vazamento de persistência | 🚫 lazy traps, mutação acidental, dirty-check | ✅ imutável, zero JPA | Alto |
| Acoplamento de compilação cross-módulo | ⚠️ `commercial` quebra se a entidade mudar | ✅ depende só dos campos que usa | Alto |
| Dependency Inversion | ⚠️ depende de concreção | ✅ depende de abstração estável | Alto |
| Esforço | ✅ zero | ⚠️ um mapeamento de 5 campos no `catalog` | Baixo |

🎯 **Recomendado e decidido:** duas interfaces (`ProcedureService` + `ProcedureProvider`), retorno `List<ProcedureView>`, e `search()` paginado no lugar de `findByName`. O custo é um arquivo de interface + um record + um mapeamento trivial; o ganho — fronteira de módulo defendida por tipo, sem vazamento de JPA e contrato de busca consistente com a ADR-001 — é estrutural e permanente, considerando que `appointment` reusará a mesma porta.

---

## Consequências positivas

- A regra "o que é um `Procedure` utilizável" vive só no `catalog`; `commercial` deleta a validação de `active` (linhas 109-111).
- `commercial`/`appointment` dependem de uma porta de 1 método, não da entidade JPA nem do CRUD — ISP e Dependency Inversion preservados por tipo.
- Busca por texto parcial habilita o fluxo real do avaliador (candidatos → seleção por id).
- Contrato de busca consistente com toda a API (ADR-001); filtros combináveis via Specification (ADR-013).
- `ProcedureView` é reaproveitado pelo `appointment` sem novo contrato.

## Consequências negativas / riscos

| Risco | Mitigação |
|---|---|
| Um mapeamento `Procedure → ProcedureView` a manter no `catalog` | Trivial (5 campos); mora onde a entidade mora |
| Dois tipos de leitura (`ProcedureResponseDTO` vs `ProcedureView`) podem confundir | Tabela "dois caminhos de leitura" documenta a fronteira; nomes distintos por superfície |
| `ProcedureProvider` como mais uma interface no módulo | Mesma classe de impl; sem custo de runtime |

---

## Alternativas consideradas

- **Uma interface só (`ProcedureService` com `resolveActiveByIds`)**: funciona e é YAGNI defensável, mas descartada porque o segundo consumidor (`appointment`) já é certo, não eventual — o split paga por si.
- **Devolver a entidade `Procedure`** (como a ADR-026 escreveu): descartada — vaza JPA/domínio do `catalog` para o `commercial`, anulando a própria fronteira que a ADR-026 defende.
- **Manter `findByName` com `Optional`/404/match exato**: descartada — viola a ADR-001 (`name` é filtro, não identificador) e inviabiliza a busca por candidatos.
- **`search` retornando `List` em vez de `Page`**: descartada por ora — a ADR-026 pede paginação; reavaliar só se a UI virar autocomplete simples.

---

## Impacto no código

| Arquivo | Mudança |
|---|---|
| `catalog/provider/ProcedureProvider.java` | **Novo** — porta cross-módulo: `List<ProcedureView> resolveActiveByIds(List<UUID>)` |
| `catalog/provider/ProcedureView.java` | **Novo** — record read-model (id, name, code, defaultPrice, estimatedDuration) |
| `catalog/service/ProcedureService.java` | Remove `isActive(...)`; `findByName(...)` → `Page<ProcedureResponseDTO> search(String name, String code, Pageable)` |
| `catalog/service/impl/ProcedureServiceImpl.java` | Implementa `resolveActiveByIds` (fail-fast + map para `ProcedureView`); `search` via Specification + `active=true`; `findByName`/`findByCode` privados; corrige args do `checkOrThrow` |
| `catalog/repository/ProcedureRepository.java` | Remove `Optional<Procedure> findByName` (match exato); passa a `JpaSpecificationExecutor<Procedure>` |
| `commercial/service/impl/DealServiceImpl.java` | Injeta `ProcedureProvider`; usa `resolveActiveByIds`; **deleta** a validação de `active` (linhas 109-111) |

---

## Referências

- ADR-026 — catálogo `Procedure` + snapshot (esta ADR revisa a prescrição `List<Procedure>`)
- ADR-002 — Interface expõe apenas o contrato do consumidor (ISP aplicado ao split)
- ADR-001 — Padrão de busca/lookup (`name`/`code` como filtros → `search`)
- ADR-013 — Specifications para listagens (busca combinável `name`/`code`/`active`)
- ADR-029 — módulo `appointment` (consumidor de `ProcedureProvider` via `estimatedDuration`)
- ADR-024 — `@TenantId` (o `@TenantId` exclui ids de outro tenant do SELECT no fail-fast)
