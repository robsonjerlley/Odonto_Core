# ADR-019: ContactLog persiste username do autor no momento da criação

**Status**: Implementado  
**Data**: 2026-06-16  
**Autores**: Arquiteto-Agent  
**Impacto**: Módulo funnel — ContactLog (entity + migration), ContactLogResponseDTO, ContactLogServiceImpl, ContactLogMapper

---

## Contexto

### Situação atual

`ContactLog` armazena `UUID userId` — referência lógica ao usuário que registrou o log, sem join físico. `ContactLogResponseDTO` expõe esse campo como `UUID userId`, sem resolução de nome.

O frontend precisa exibir o autor de cada log por nome legível (ex.: "João Silva"), não por UUID. Sem essa resolução, a UI exigiria chamadas adicionais para `GET /users/{id}` a fim de obter o nome de cada autor — uma por log exibido na listagem.

---

## Decisão

Persistir `String username` diretamente na tabela `contact_logs`, preenchido uma única vez no `create()` a partir do `currentUser.getName()`. Leituras (`findById`, `search`) obtêm o campo direto da coluna — zero query extra.

### Raciocínio

`ContactLog` é imutável após a criação (ADR-003) e `userId` é **sempre** o `currentUser` no momento do `create()`. Portanto:

- O autor nunca muda — imutabilidade garante isso
- `currentUser.getName()` no `create()` é o nome correto do autor para toda a vida do registro
- Não há cenário onde um log tenha um autor diferente do `currentUser` no momento da criação

Consequência direta: o nome pode ser gravado junto com o log, eliminando qualquer necessidade de lookup na leitura. A abordagem `@Transient` (populado em memória a cada leitura) foi considerada e descartada — ver Alternativas.

### Implementação

**`ContactLog` — campo persistido:**

```java
@Column
private String username;
```

**`ContactLogServiceImpl.create()` — já correto:**

```java
ContactLog contactLog = ContactLog.builder()
        .ticketId(ticket.getId())
        .userId(userId)
        .username(user.getName())   // gravado uma vez, nunca alterado
        ...
        .build();
```

**`findById()` e `search()`** — nenhuma alteração. O mapper lê `username` direto da coluna.

**`ContactLogMapper`** — nenhuma alteração. MapStruct mapeia `getUsername()` → `username` por convenção de nome.

---

## Consequências Positivas

- Zero query extra em leituras — `username` está na coluna, disponível sem join
- `UserRepository` desnecessário no service — sem nova dependência
- `create()` já tem `currentUser` em memória — nenhum custo adicional na escrita
- Snapshot histórico correto: o log registra o nome do autor como era no momento da criação; uma eventual mudança de nome do usuário não altera o histórico (comportamento desejável em logs de auditoria)
- Elimina N+1 HTTP no frontend

## Consequências Negativas / Riscos

- Registros existentes terão `username = null` — o projeto usa `ddl-auto=update`; Hibernate adiciona a coluna automaticamente no próximo start. Aceitável: o frontend trata `null` como "Usuário desconhecido"

---

## Alternativas Consideradas

- **Frontend resolve via `GET /users/{id}`**: descartado. Introduz N+1 HTTP, pressiona o frontend a implementar cache.
- **`@Transient String username` populado no service a cada leitura**: descartado. Requer `UserRepository` injetado, adiciona uma query por log lido, e ignora o fato de que a imutabilidade do `ContactLog` torna a resolução em escrita suficiente e mais eficiente.
- **Lookup via `UserRepository` no `create()` após `save()`**: descartado. `currentUser` já está em memória — fazer uma query para buscar o mesmo usuário é redundante.
- **Alterar assinatura do mapper para `toResponseDTO(ContactLog, String)`**: descartado. Quebra `search()` que usa referência de método sobre `Page.map()`.

---

## Referências Cruzadas

- `ADR-003` — ContactLog é imutável; garante que `username` gravado na criação é definitivo
- `ContactLog.java` — trocar `@Transient` por `@Column`; Hibernate (`ddl-auto=update`) cria a coluna automaticamente
- `ContactLogServiceImpl.java` — `create()` já correto; `findById()` e `search()` sem alteração
- `frontend-integration-contract.md` — `ContactLogResponse` TypeScript com campo `username: string`