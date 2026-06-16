# ADR-019: ContactLogResponseDTO expõe userName resolvido no backend

**Status**: Aceito  
**Data**: 2026-06-16  
**Autores**: Arquiteto-Agent  
**Impacto**: Módulo funnel — ContactLog (entity), ContactLogResponseDTO, ContactLogServiceImpl, ContactLogMapper

---

## Contexto

### Situação atual

`ContactLog` armazena `UUID userId` — referência lógica ao usuário que registrou o log, sem join físico (padrão estabelecido no projeto). `ContactLogResponseDTO` expõe esse campo como `UUID userId`, sem resolução de nome.

O frontend precisa exibir o autor de cada log por nome legível (ex.: "João Silva"), não por UUID. Sem essa resolução, a UI exigiria chamadas adicionais para `GET /users/{id}` a fim de obter o nome de cada autor — uma por log exibido na listagem.

### Problema

A ausência de `userName` no DTO cria dois caminhos ruins:

1. **N+1 HTTP no frontend**: uma lista de 20 logs dispara 20 chamadas `GET /users/{id}`. Inevitavelmente leva a cache local no frontend, com stale data e complexidade de invalidação crescente.
2. **Responsabilidade de resolução no cliente**: regra de negócio ("este log foi criado por tal pessoa") sai da fonte de verdade (backend) e vai para o consumidor — violando o princípio de separação de responsabilidades.

---

## Decisão

Adicionar `String userName` ao `ContactLogResponseDTO`, resolvido via lookup por `userId` no `ContactLogServiceImpl` antes do mapeamento.

### Por que a entidade precisa de `@Transient String userName`

`ContactLogServiceImpl.create()` e `search()` constroem o response via `contactLogMapper.toResponseDTO(contactLog)` — o mapper opera sobre a entidade com assinatura de um único argumento. Alterar essa assinatura para `toResponseDTO(ContactLog, String)` quebraria o `search()`, que usa referência de método (`.map(contactLogMapper::toResponseDTO)`) sobre a Page retornada pelo repositório.

A única forma de injetar `userName` no fluxo sem alterar a assinatura do mapper é adicionar o campo `@Transient` na entidade: JPA não persiste, Lombok expõe via `@Getter`, MapStruct lê automaticamente.

### Implementação

**`ContactLog` — campo transiente:**

```java
@Transient
private String userName;
```

O campo não tem coluna correspondente na tabela `contact_logs`. É preenchido em memória pelo service antes de chamar o mapper.

**`ContactLogResponseDTO` — novo campo:**

```java
public record ContactLogResponseDTO(
    UUID id,
    UUID ticketId,
    UUID userId,
    String userName,
    ContactChannel channel,
    String note,
    TicketStatus statusBefore,
    TicketStatus statusAfter,
    LocalDateTime occurredAt,
    LocalDateTime createdAt
) {}
```

**`ContactLogServiceImpl` — resolver e setar antes de mapear:**

O service injeta `UserRepository`. Antes de chamar o mapper, resolve o nome e seta no objeto da entidade:

```java
// create()
ContactLog saved = contactLogRepository.save(contactLog);
userRepository.findById(saved.getUserId())
        .map(User::getName)
        .ifPresentOrElse(saved::setUserName, () -> saved.setUserName("Usuário removido"));
return contactLogMapper.toResponseDTO(saved);

// search() — resolver no stream antes do map
return contactLogRepository.findAll(spec, pageable)
        .map(log -> {
            userRepository.findById(log.getUserId())
                    .map(User::getName)
                    .ifPresentOrElse(log::setUserName, () -> log.setUserName("Usuário removido"));
            return contactLogMapper.toResponseDTO(log);
        });
```

**`ContactLogMapper`** — nenhuma alteração necessária. MapStruct detecta o getter `getUserName()` na entidade e mapeia para o campo `userName` do DTO automaticamente por convenção de nome.

---

## Consequências Positivas

- Elimina N+1 HTTP no frontend — toda a informação necessária para renderizar o log chega em uma única resposta
- Regra de resolução centralizada no backend — frontend não precisa conhecer a estrutura interna de `userId`
- Contrato do DTO humanamente legível — `userName` é autodocumentado
- Join por PK (`userId`) é O(1) no banco; custo operacional desprezível no volume atual

## Consequências Negativas / Riscos

- Join extra por `userId` a cada leitura de log — mitigável com Redis Cache (spec já existente no backlog) quando o volume justificar
- Se o usuário for deletado (ou desativado) futuramente, o lookup pode não encontrar o registro — tratar com fallback `"Usuário removido"` ou equivalente

---

## Alternativas Consideradas

- **Frontend resolve via `GET /users/{id}`**: descartado. Introduz N+1 HTTP, pressiona o frontend a implementar cache, e desloca responsabilidade de resolução de dados para o cliente HTTP.
- **Embutir objeto `UserSummaryDTO` (id + name + sector)**: descartado como solução primária — over-engineering para o caso de uso atual. Se futuramente o frontend precisar de mais campos do autor (ex.: setor), a decisão pode evoluir para um objeto aninhado sem quebrar o contrato existente.
- **Alterar assinatura do mapper para `toResponseDTO(ContactLog, String userName)`**: descartado. Quebra `search()` que usa referência de método sobre `Page.map()`. Exigiria refatorar o fluxo de paginação em todos os call sites.
- **Resolver via `@Context UserRepository` no mapper**: descartado. Introduz dependência de repositório dentro do mapper, violando sua responsabilidade de transformação pura 1:1.

---

## Referências Cruzadas

- `ADR-003` — ContactLog é imutável; `userId` nunca muda após criação — `userName` é estável por construção
- `ADR-013` — padrão de Specifications; não impacta leitura por ID
- `spec-redis-cache.md` — cache de usuários pode ser aplicado futuramente para eliminar o lookup a cada leitura
- `ContactLogResponseDTO.java` — ponto de implementação do novo campo
- `ContactLogServiceImpl.java` — ponto de resolução do `userName`
- `frontend-integration-contract.md` — `ContactLogResponse` TypeScript precisa ser atualizado com campo `userName: string`
