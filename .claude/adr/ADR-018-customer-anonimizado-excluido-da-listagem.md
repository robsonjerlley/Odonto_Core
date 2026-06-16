# ADR-019: Pacientes anonimizados excluídos da listagem padrão de clientes

**Status**: Aceito  
**Data**: 2026-06-15  
**Autores**: Arquiteto-Agent  
**Impacto**: Módulo funnel — CustomerSpecifications, CustomerServiceImpl, CustomerServiceTest

---

## Contexto

### Situação atual

A ADR-006 estabeleceu que `DELETE /customers/{id}` executa anonimização de PII em vez de deleção física. O cliente anonimizado permanece no banco com `name = "CLIENTE ANONIMIZADO"`, `cpf = null`, `phone = null`, `email = null` e `anonymized = true`.

`CustomerServiceImpl.search()` compõe Specifications via `where(byScope).and(hasPhoneNumber).and(hasName).and(hasAdsChannel)` — sem nenhum predicado que filtre `anonymized = true`. Como resultado, registros anonimizados aparecem na aba de pacientes do frontend misturados aos registros ativos.

Esses registros não têm qualquer utilidade operacional para a clínica: sem nome, CPF, telefone ou e-mail, não é possível agendamento, contato ou qualquer ação de negócio. Sua presença na listagem polui a tela, distrai os operadores e pode causar tentativas de interação com um registro inerte.

### Por que isso não foi tratado na ADR-006

A ADR-006 apontou que "o frontend precisa tratar o estado visualmente via campo `anonymized: boolean`" — delegando a responsabilidade ao cliente HTTP. Essa abordagem é inadequada porque:

1. **Filtragem no cliente é insegura**: um frontend mal implementado pode exibir os registros mesmo com o campo presente.
2. **Filtragem no servidor é a fonte da verdade**: a regra de negócio "clientes anonimizados não aparecem na operação" deve viver no backend.
3. **Separação de responsabilidade**: o frontend não deve precisar conhecer o estado interno de anonimização para renderizar uma listagem correta.

---

## Decisão

Adicionar o predicado `notAnonymized()` em `CustomerSpecifications` e aplicá-lo obrigatoriamente em `CustomerServiceImpl.search()`, excluindo clientes com `anonymized = true` de toda listagem padrão.

### Implementação

**`CustomerSpecifications.java` — novo predicado:**

```java
public static Specification<Customer> notAnonymized() {
    return (root, query, cb) -> cb.isFalse(root.get("anonymized"));
}
```

**`CustomerServiceImpl.search()` — aplicar na chain:**

```java
Specification<Customer> spec = Specification
    .where(byScope(scope, user))
    .and(notAnonymized())           // sempre forçado
    .and(hasPhoneNumber(phone))
    .and(hasName(name))
    .and(hasAdsChannel(adsChannel));
```

O predicado é posicionado imediatamente após `byScope` e antes dos filtros de negócio, tornando explícita sua natureza de filtro estrutural, não de busca.

### Comportamento de `findById`

`GET /customers/{id}` com UUID de um cliente anonimizado continua retornando o registro (HTTP 200). Esse endpoint é de acesso direto por chave — não é uma listagem operacional. Módulos internos (analytics, deals, tickets) precisam dessa rota para resolução de referências. Não há alteração nesse comportamento.

---

## Consequências Positivas

- A aba de pacientes exibe apenas registros operacionalmente válidos
- Regra de negócio centralizada no backend — frontend não precisa saber do campo `anonymized` para renderizar a listagem
- Implementação trivial: 1 predicado + 1 linha na chain de Specifications
- Sem impacto em ContactLogs, LeadTickets ou Deals — continuam referenciando o UUID do cliente anonimizado normalmente (ADR-003, ADR-006)
- Sem impacto em `GET /customers/{id}` — resolução por UUID direto permanece funcional

## Consequências Negativas / Riscos

- `ADM_SYSTEM` perde visibilidade de clientes anonimizados via `GET /customers`. Mitigação aceitável: auditoria de anonimizações é um fluxo de compliance separado — não um caso de uso operacional. Se futuramente necessário, um endpoint dedicado `GET /customers/anonymized` com RBAC restrito (ADM_SYSTEM, GLOBAL) pode ser adicionado sem alterar esta decisão.

---

## Alternativas Consideradas

- **Filtro opcional via query param `?includeAnonymized=true`**: descartado. Transfere a responsabilidade da exclusão para o frontend e cria risco de exibição acidental se o param for omitido. A regra de exclusão é absoluta para o fluxo operacional.
- **Endpoint separado `GET /customers/anonymized`**: descartado como solução primária. Resolve a separação de responsabilidade mas não corrige o problema imediato — o `search()` continuaria retornando anonimizados. Pode ser implementado no futuro como complemento para auditoria LGPD.
- **Tratar no frontend via campo `anonymized`**: descartado. Abordagem já indicada na ADR-006 como mitigação temporária; insuficiente como decisão arquitetural definitiva — regra de negócio não deve depender do cliente HTTP para ser aplicada.

---

## Referências Cruzadas

- `ADR-003` — ContactLog imutável; logs de clientes anonimizados permanecem intactos
- `ADR-006` — anonimização de PII; define o campo `anonymized` e o comportamento do `DELETE /customers/{id}`
- `ADR-013` — padrão de Specifications para listagens scope-aware; `notAnonymized()` segue o mesmo padrão de composição
- `CustomerSpecifications.java` — ponto de implementação do predicado
- `CustomerServiceImpl.java` — ponto de aplicação na chain do `search()`
- `CustomerServiceTest.java` — adicionar teste validando que `search()` não retorna clientes anonimizados
