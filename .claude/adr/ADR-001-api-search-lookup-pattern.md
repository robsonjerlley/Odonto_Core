# ADR-001: Padrão de Busca e Identificação em APIs REST

**Status**: Aceito
**Data**: 2026-05-27
**Autores**: Arquiteto-Agent + Product Owner Agent
**Impacto**: Global — todos os módulos da API

---

## Contexto

Durante a implementação do módulo funnel, constatou-se que o `CustomerController` expunha endpoints de busca com paradigmas inconsistentes: path params para CPF e nome, query param para phone, endpoint separado para ID. O mesmo padrão fragmentado foi identificado nos demais módulos:

- `GET /users/findByUsername/{username}` — busca por atributo no path
- `GET /users/findBySector/{sector}` — filtro no path
- `GET /users/findBySectorAndRole/{sector}/{role}` — filtros combinados no path
- `GET /tickets/findByCustomer/{customerId}` — filtro no path
- `GET /tickets/ticketStatus/{status}` — filtro no path
- `GET /contact-logs/findByTicketId/{id}` — filtro no path

A mistura de paradigmas cria contratos imprevisíveis, dificulta extensão e impede geração automática de clientes SDK. O projeto encontra-se em estágio pré-publicação, sem contrato público estabelecido — este é o momento de fixar o padrão antes de qualquer consumidor externo existir.

---

## Decisão

Dois e apenas dois padrões de acesso GET são permitidos nesta API:

### Padrão 1 — Identificação por chave única (Path Param)

**Quando usar**: o valor identifica *univocamente* um recurso. A ausência de resultado é um erro (404), não uma lista vazia.

**Formato**: `GET /resource/{uniqueKey}`

**Retorno**: objeto único ou HTTP 404

**Exemplos canônicos do projeto**:
```
GET /api/v1/customers/{id}          → Customer pelo UUID interno
GET /api/v1/customers/cpf/{cpf}     → Customer pelo CPF (ver seção CPF abaixo)
GET /api/v1/users/{id}              → User pelo UUID interno
GET /api/v1/users/username/{username} → User pelo username (identificador de login)
GET /api/v1/tickets/{id}            → LeadTicket pelo UUID interno
GET /api/v1/contact-logs/{id}       → ContactLog pelo UUID interno
GET /api/v1/deal/{id}               → Deal pelo UUID interno
```

**Critério de decisão**: o campo é o *nome próprio* do recurso — sua ausência torna o recurso não encontrável (404). Se o resultado pode ser uma lista vazia, o campo é um filtro, não um identificador.

---

### Padrão 2 — Filtragem por atributos (Query Params)

**Quando usar**: o valor refina uma listagem. O resultado é sempre uma lista, mesmo que contenha zero ou um item.

**Formato**: `GET /resource?param=value[&param2=value2]`

**Retorno**: `List<DTO>` (pode ser vazia) com HTTP 200

**Exemplos canônicos do projeto**:
```
GET /api/v1/customers?name=&phone=&adChannel=
GET /api/v1/users?sector=&role=
GET /api/v1/tickets?customerId=&status=&assignedTo=
GET /api/v1/contact-logs?ticketId=
```

**Critério de decisão**: qualquer campo que não seja identificador único do recurso é query param. Inclui: nome, telefone, setor, status, canal de anúncio, responsável.

---

### Exceção aceita: CPF em sub-path

CPF permanece em `GET /customers/cpf/{cpf}` — não como query param — por duas razões de negócio inegociáveis:

1. **Semântica de identidade**: CPF é um identificador fiscal único do cidadão. A ausência de resultado é 404, não lista vazia. Comportamento idêntico ao UUID interno.
2. **Invariante de agendamento**: a transição de qualquer `LeadTicket` para status `SCHEDULED` exige CPF preenchido no `Customer`. O endpoint `/cpf/{cpf}` serve como ponto de verificação explícita antes do agendamento — o chamador espera 200 (pode agendar) ou 404 (CPF não cadastrado, bloqueado).

---

## Regra sobre Nomes de Rotas

Prefixos semânticos (`findBy`, `search`, `get`, `list`, `exists`) são **proibidos em URLs**. O método HTTP e a estrutura path/query já expressam a intenção.

| Proibido | Correto |
|----------|---------|
| `GET /customers/findByCpf/{cpf}` | `GET /customers/cpf/{cpf}` |
| `GET /users/findByUsername/{username}` | `GET /users/username/{username}` |
| `GET /users/findBySector/{sector}` | `GET /users?sector=` |
| `GET /tickets/ticketStatus/{status}` | `GET /tickets?status=` |
| `GET /contact-logs/findByTicketId/{id}` | `GET /contact-logs?ticketId=` |

---

## Comportamento com Múltiplos Query Params Simultâneos

Quando múltiplos query params são enviados na mesma requisição (`?name=João&phone=999`), o comportamento atual é determinístico por prioridade de verificação no service:

```
cpf > phone > name > adChannel  (CustomerService)
sector > role                    (UserService)
customerId > status > assignedTo (TicketService)
```

Esta prioridade é implícita no código via if/else sequencial. **Ela deve ser documentada no JavaDoc do método `search()` de cada service**. Suporte a filtros combinados (AND) é backlog futuro via Specification Pattern ou JPA Criteria.

---

## Impacto por Módulo

### Módulo identity — Users

| Endpoint deprecado | Endpoint novo |
|--------------------|---------------|
| `GET /users/findByUsername/{username}` | `GET /users/username/{username}` |
| `GET /users/findBySector/{sector}` | `GET /users?sector=` |
| `GET /users/findBySectorAndRole/{sector}/{role}` | `GET /users?sector=&role=` |
| `GET /users/existsByUsername/{username}` | Absorvido: `GET /users/username/{username}` retorna 404 quando não existe |

### Módulo funnel — Customers

| Endpoint deprecado | Endpoint novo |
|--------------------|---------------|
| `GET /customers/username/{username}` | `GET /customers?name=` |

Mantidos sem alteração:
- `GET /customers/{id}` — path param UUID
- `GET /customers/cpf/{cpf}` — exceção aceita (ver seção CPF)

### Módulo funnel — Tickets

| Endpoint deprecado | Endpoint novo |
|--------------------|---------------|
| `GET /tickets/findByCustomer/{customerId}` | `GET /tickets?customerId=` |
| `GET /tickets/ticketStatus/{status}` | `GET /tickets?status=` |
| `GET /tickets/assignedToUser/{userId}` | `GET /tickets?assignedTo=` |

### Módulo funnel — Contact Logs

| Endpoint deprecado | Endpoint novo |
|--------------------|---------------|
| `GET /contact-logs/findByTicketId/{id}` | `GET /contact-logs?ticketId=` |

---

## Consequências Positivas

- Contrato de API consistente e previsível em todos os módulos
- Extensão de filtros requer apenas um novo `@RequestParam` no controller e uma linha no service — sem nova rota
- Frontend/integradores operam com um único paradigma de busca
- Facilita documentação OpenAPI e geração automática de clientes SDK futuros
- Elimina rotas com prefixos semânticos redundantes (`findBy`, `search`)

## Consequências Negativas / Riscos

- Refatoração dos controllers e services existentes — breaking change interno (sem contrato público ainda, custo zero)
- Comportamento com múltiplos params simultâneos é implicitamente ordenado — deve ser documentado

## Alternativas Consideradas

- **Manter endpoints separados por atributo**: descartado — cada novo filtro demanda nova rota, proliferação sem limite
- **GraphQL para queries flexíveis**: descartado — over-engineering para o estágio atual, sem maturidade no time para o paradigma
- **CQRS com query objects estruturados**: descartado — correto arquiteturalmente mas prematuro para o volume atual

---

## Referências Cruzadas

- `us-fundacional.md` — US-FUND-01 especifica `GET /customers?phone=` como o primeiro uso deste padrão
- `BACKEND.md` — tabelas de API atualizadas para refletir esta decisão
- `CLAUDE.md` — padrões do projeto atualizados com referência a este ADR