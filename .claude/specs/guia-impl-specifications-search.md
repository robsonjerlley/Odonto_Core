# Guia de implementação — JPA Specifications no `search()` scope-aware

**Decisão de origem:** ADR-013
**Público:** dev que nunca usou Specifications — guia conceitual + esqueleto, não solução pronta
**Modo:** você implementa; este doc explica o que cada peça faz e por quê

---

## 0. A intuição: o que é uma Specification e que problema resolve

O Spring Data tem 3 jeitos de consultar:

1. **Derived query** — `findByStatus(...)`. O nome do método **é** a query. Ótimo para 1 condição
   fixa. Péssimo para condições dinâmicas: cada combinação vira um método novo (explosão).
2. **`@Query`** — você escreve JPQL/SQL na anotação. Estático; condicional opcional vira `:x IS NULL OR ...` (frágil).
3. **Specification** — você descreve **um pedaço de `WHERE`** como objeto, em tempo de execução, e
   **compõe** vários pedaços com `.and()`/`.or()`. O Spring monta **uma** query com todos juntos.

Uma `Specification<T>` é, na prática, uma função que recebe três coisas do JPA Criteria API e devolve
um `Predicate` (um pedaço de `WHERE`):

```
(root, query, criteriaBuilder) -> Predicate
```

- `root` — a "tabela" da entidade na query (de onde você pega colunas: `root.get("status")`).
- `query` — a query inteira (usada para `distinct`, subqueries).
- `criteriaBuilder` (`cb`) — a fábrica de predicados (`cb.equal(...)`, `cb.in(...)`, `cb.exists(...)`).

**Por que resolve o seu caso:** scope e filtros viram pedaços de `WHERE` independentes, e você junta
todos num `AND`. Resultado: **1 SELECT + 1 COUNT** (ver ADR-013 sobre o porquê do COUNT), com tudo
resolvido no banco — sem método por combinação, sem filtrar em memória.

> **Por que você "nunca fez":** o `switch` por scope que está no código hoje (e o esboço da ADR-012,
> linhas 43-52) **escolhe um método de repositório** por scope. Isso só funciona com UMA dimensão.
> Specification troca "escolher método" por "montar predicado" — e predicado você soma quantos quiser.

---

## 1. Pré-requisito: o repositório precisa saber executar Specification

Hoje seus repositórios estendem só `JpaRepository<T, ID>`. Para usar `findAll(spec, pageable)` eles
precisam estender também **`JpaSpecificationExecutor<T>`**:

```java
public interface LeadTicketRepository
        extends JpaRepository<LeadTicket, UUID>, JpaSpecificationExecutor<LeadTicket> {
    // os derived methods usados em analytics/RecycleJob CONTINUAM aqui (findByStatusAndPendingAtBefore etc.)
    // os derived methods que existiam só para o search (findByCurrentSector, findByCurrentSectorIn,
    // findByCreatedBy, findByStatus(pageable), findByCustomerId(pageable)) podem SAIR depois.
}
```

> `JpaSpecificationExecutor` te dá de graça: `findAll(Specification, Pageable)`, `findAll(Specification)`,
> `count(Specification)`, etc.

Fazer o mesmo em `CustomerRepository`, `ContactLogRepository` e `UserRepository`.

---

## 2. A classe de fragmentos: `*Specifications`

Crie uma classe por entidade, ao lado do repositório (camada de persistência). Cada método estático
devolve **um** `Specification<T>` — um pedaço de `WHERE`. **Convenção de ouro:** se o parâmetro for
`null`, devolva `null` — o Spring trata `null` como "não filtra" quando você usa `Specification.where(...)`.

Esqueleto (implemente os corpos — deixei 1 pronto como referência e os outros como contrato):

```java
public final class LeadTicketSpecifications {

    private LeadTicketSpecifications() {} // classe utilitária, não instanciável

    // ---- FILTROS DE NEGÓCIO (vêm dos query params) ----

    // referência implementada:
    public static Specification<LeadTicket> hasStatus(TicketStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<LeadTicket> hasCustomerId(UUID customerId) {
        // TODO: igual ao de cima, comparando root.get("customerId")
    }

    public static Specification<LeadTicket> assignedTo(UUID userId) {
        // TODO: root.get("assignedTo")
    }

    // ---- FRAGMENTOS DE SCOPE (vêm do PermissionScope) ----

    public static Specification<LeadTicket> createdBy(UUID userId) {
        // TODO: root.get("createdBy")
    }

    public static Specification<LeadTicket> currentSector(Sector sector) {
        // TODO: root.get("currentSector")
    }

    public static Specification<LeadTicket> currentSectorIn(List<Sector> sectors) {
        // TODO: root.get("currentSector").in(sectors)  — use cb.in(...) ou root.get(...).in(...)
    }
}
```

Cada fragmento é **unit-testável isolado** e **reutilizável** em qualquer endpoint futuro.

---

## 3. Traduzir o scope em Specification (o seam reutilizável)

Aqui mora a virada de chave: o seu `switch` deixa de chamar repositório e passa a **devolver predicado**.
Pode ser um método estático na própria `*Specifications`:

```java
public static Specification<LeadTicket> byScope(PermissionScope scope, User user) {
    return switch (scope) {
        case GLOBAL -> null;                                  // sem WHERE de scope
        case SECTOR -> currentSector(user.getSector());
        case INTAKE -> currentSectorIn(List.of(LEADS, ATTENDANT));
        case OWN    -> createdBy(user.getId());
    };
}
```

> Note: `GLOBAL` devolve `null` (não restringe). `Specification.where(null)` é seguro — não adiciona WHERE.

---

## 4. O `search()` reescrito: compor, não escolher

```java
@Override
@Transactional(readOnly = true)
public Page<LeadTicketResponseDTO> search(UUID customerId, TicketStatus status,
                                          UUID assignedTo, Pageable pageable) {
    User user = securityUtils.getCurrentUser();

    PermissionScope scope = permissionService.getScope(user, TICKET, READ)
            .orElseThrow(() -> new AccessDeniedException("Access denied"));

    Specification<LeadTicket> spec = Specification
            .where(LeadTicketSpecifications.byScope(scope, user))   // autorização (pode ser null)
            .and(LeadTicketSpecifications.hasCustomerId(customerId)) // filtro (null = ignora)
            .and(LeadTicketSpecifications.hasStatus(status))
            .and(LeadTicketSpecifications.assignedTo(assignedTo));

    return ticketRepository.findAll(spec, pageable)
            .map(ticketMapper::toResponseDTO);
}
```

O que isso faz no banco: `WHERE <scope> AND <customerId?> AND <status?> AND <assignedTo?>`, com os
`null` simplesmente não entrando. **Filtros agora são cumulativos (AND)** — é a mudança de contrato
da ADR-013 (§7.4 atualizado).

Depois disso: **apague** os privados órfãos `findAll`, `findByCustomer`, `findByStatus`,
`findByAssignedToUser` e a dependência `userRepository` (resolve L1).

---

## 5. A parte difícil: Customer e ContactLog com `EXISTS` (scope SECTOR/INTAKE)

Customer/ContactLog **não têm** coluna de setor — o setor é do `LeadTicket`. O `EXISTS` pergunta:
"existe algum ticket deste cliente/log que esteja no setor permitido?". É uma **subquery correlacionada**.

Esqueleto comentado (CustomerSpecifications — implemente os corpos):

```java
public static Specification<Customer> hasTicketInSectors(List<Sector> sectors) {
    if (sectors == null || sectors.isEmpty()) return null;
    return (root, query, cb) -> {
        // subquery: SELECT 1 FROM LeadTicket t WHERE t.customerId = customer.id AND t.currentSector IN (:sectors)
        Subquery<UUID> sub = query.subquery(UUID.class);
        Root<LeadTicket> ticket = sub.from(LeadTicket.class);
        sub.select(ticket.get("id"));
        sub.where(
            cb.equal(ticket.get("customerId"), root.get("id")),       // correlação com o Customer
            ticket.get("currentSector").in(sectors)
        );
        return cb.exists(sub);
    };
}

// SECTOR é só um caso de IN com um elemento:
public static Specification<Customer> hasTicketInSector(Sector sector) {
    return sector == null ? null : hasTicketInSectors(List.of(sector));
}
```

E o `byScope` do Customer:

```java
public static Specification<Customer> byScope(PermissionScope scope, User user) {
    return switch (scope) {
        case GLOBAL -> null;
        case OWN    -> createdBy(user.getId());                    // Customer.createdBy
        case SECTOR -> hasTicketInSector(user.getSector());
        case INTAKE -> hasTicketInSectors(List.of(LEADS, ATTENDANT));
    };
}
```

ContactLog é o mesmo, mudando a correlação para `t.id = log.ticketId` e o OWN para `userId`.

> **Cuidado real:** `EXISTS` pode duplicar linhas em alguns mapeamentos com join — aqui não duplica
> porque a subquery é isolada (não é um join no root). Não precisa de `query.distinct(true)` neste caso.

---

## 6. Ordem de implementação sugerida (incremental, testável a cada passo)

```
1. LeadTicketRepository  → estende JpaSpecificationExecutor          [compila, nada quebra]
2. LeadTicketSpecifications → fragmentos de filtro + scope + byScope  [escreve testes unitários dos fragmentos]
3. LeadTicketServiceImpl.search() → compõe spec; remove órfãos + userRepository
4. Validar GET /tickets com combinações de filtro (Postman/teste de integração)
5. Repetir 1-4 para Customer e ContactLog (com EXISTS)
6. (opcional) User search migra para o mesmo padrão
```

---

## 7. Como testar um fragmento isoladamente (a vantagem do padrão)

Você não precisa subir o contexto inteiro: monta um `Specification`, passa para o repositório num teste
`@DataJpaTest`, e confere o resultado. Em nível ainda mais baixo, dá para testar a lógica de "param
null → null" sem banco nenhum:

```java
assertNull(LeadTicketSpecifications.hasStatus(null));
assertNotNull(LeadTicketSpecifications.hasStatus(TicketStatus.NEW));
```

O teste de que o `WHERE` sai correto fica no `@DataJpaTest` (com Testcontainers, que o projeto já tem).

---

## 8. Armadilhas (onde você provavelmente vai tropeçar — e por quê)

| Sintoma | Causa | Como evitar |
|--------|------|-------------|
| `NullPointerException` ao compor | esqueceu de devolver `null` no param ausente e fez `.and()` de algo inválido | sempre `if (param == null) return null;` no topo do fragmento |
| Filtro "não funciona" | comparou contra `String` quando a coluna é enum, ou nome de propriedade errado em `root.get("...")` | `root.get` usa o **nome do campo Java**, não a coluna do banco |
| Página com `totalElements` errado | tentou filtrar a lista em memória depois do `findAll` | nunca filtre depois; tudo no `Specification` |
| `EXISTS` retorna vazio sempre | correlação errada (`t.customerId = t.id` em vez de `= customer.id`) | a correlação liga a subquery ao `root` externo |
| Boot falha com `PropertyReferenceException` | nome de campo digitado errado em derived method | use Specification para dinâmico; reserve derived só para o fixo |

---

## Referências

- `ADR-013` — decisão e tabela de predicados por scope/entidade
- `ADR-012` (linhas 43-52) — o padrão antigo (switch escolhendo método) que este guia substitui
- `frontend-integration-contract.md` §7.3/§7.4/§7.5 — semântica nova (filtros cumulativos + visibilidade por scope)
- `avaliacao-backend-2026-06-06.md` — M1, M2, L1 que esta implementação resolve