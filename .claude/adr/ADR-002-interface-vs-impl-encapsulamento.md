# ADR-002: Interface de Service deve expor apenas o contrato do consumidor

**Status**: Aceito  
**Data**: 2026-05-28  
**Autores**: Arquiteto-Agent  
**Impacto**: Módulo funnel — CustomerService; padrão aplicável a todos os services do projeto

---

## Contexto

Durante a implementação de `CustomerServiceImpl`, o método `search(phone, name, adChannel)` foi introduzido para unificar a busca no `CustomerController` (conforme ADR-001). Internamente, `search()` despacha a chamada para métodos auxiliares — `findByPhone`, `findByName`, `findByAdChannel` e `findAll` — usando uma cadeia `if/else` por prioridade.

Esses quatro métodos auxiliares foram declarados na interface `CustomerService`, apesar de nenhum consumidor externo (controller, outro service) os chamar diretamente. O único caller de cada um deles é o próprio `search()`, dentro da mesma classe.

O resultado é uma interface com seis operações que nenhum consumidor usa, expondo detalhes de implementação interna como parte do contrato público.

---

## Decisão

Os métodos `findAll`, `findByName`, `findByPhone` e `findByAdChannel` são removidos da interface `CustomerService` e redeclarados como `private` em `CustomerServiceImpl`.

### Interface resultante (`CustomerService`)

```java
public interface CustomerService {
    CustomerResponseDTO create(CustomerCreateRequestDTO dto);
    CustomerResponseDTO update(UUID id, CustomerUpdateRequestDTO dto);
    List<CustomerResponseDTO> search(String phone, String name, AdsChannel adChannel);
    CustomerResponseDTO findById(UUID id);
    CustomerResponseDTO findByCpf(String cpf);
    void deleteById(UUID id);
}
```

### Implementação resultante (`CustomerServiceImpl`)

Os quatro métodos tornam-se `private` sem `@Override`. A anotação `@Transactional(readOnly = true)` é removida deles — eles participam da transação aberta pelo `search()` público, que já carrega essa anotação.

---

## Critério de decisão aplicado

> **Um método pertence à interface se, e somente se, algum consumidor externo à classe de implementação o chama diretamente.**

| Método | Controller chama? | Outro service chama? | Pertence à interface? |
|--------|:-----------------:|:--------------------:|:---------------------:|
| `create` | ✅ | — | ✅ |
| `update` | ✅ | — | ✅ |
| `search` | ✅ | — | ✅ |
| `findById` | ✅ | — | ✅ |
| `findByCpf` | ✅ | — | ✅ |
| `deleteById` | ✅ | — | ✅ |
| `findAll` | ❌ | ❌ | ❌ — `private` |
| `findByName` | ❌ | ❌ | ❌ — `private` |
| `findByPhone` | ❌ | ❌ | ❌ — `private` |
| `findByAdChannel` | ❌ | ❌ | ❌ — `private` |

---

## Nota sobre `@Transactional` em métodos privados

Spring AOP opera via proxy: apenas métodos **públicos** são interceptados. Métodos `private` nunca recebem o advice transacional do proxy, independentemente de terem `@Transactional`.

No cenário pós-refactor, isso é correto por design:

- `search()` — público, `@Transactional(readOnly = true)` — abre a transação
- `findByPhone`, `findByName`, `findByAdChannel`, `findAll` — privados, sem anotação — executam dentro da transação já aberta pelo chamador

Não há perda de garantia transacional. A anotação nos métodos privados era inoperante antes mesmo desta decisão.

---

## Consequências Positivas

- Interface expressa apenas o contrato do consumidor — mais fácil de entender e de mockar em testes
- Estratégias internas de `search()` ficam livres para mudar sem impactar o contrato público
- Previne que futuros controllers ou services chamem `findByPhone` diretamente, contornando a prioridade definida em `search()` e quebrando a invariante do ADR-001
- Reduz superfície de mock nos testes de integração do controller

## Consequências Negativas / Riscos

- Se um futuro consumer precisar de `findAll` isoladamente, o método precisará ser re-promovido à interface. Custo baixo e deliberado — YAGNI aplicado.
- Desenvolvedores não familiarizados com o critério podem re-adicionar métodos privados à interface por hábito. Mitigação: este ADR serve como referência.

---

## Alternativas Consideradas

- **Manter todos os métodos na interface**: descartado — viola ISP e expõe detalhes de implementação como contrato público
- **Extrair os métodos para uma classe auxiliar `CustomerSearchHelper`**: descartado — over-engineering para quatro métodos simples sem reuso fora desta classe
- **Criar uma segunda interface `CustomerQueryService`**: descartado — separação sem benefício real no volume atual; o critério de consumidor externo já resolve o problema de forma direta

---

## Padrão aplicável ao projeto

Esta decisão estabelece o critério para todos os services do projeto:

> Interfaces de service definem o contrato entre camadas (controller → service, service → service). Métodos usados exclusivamente como sub-rotinas internas da implementação devem ser `private` na classe concreta — nunca declarados na interface.

---

## Referências Cruzadas

- `ADR-001` — define o padrão de busca unificada via `search()` que motivou esta decisão
- `CustomerService.java` — interface resultante desta decisão
- `CustomerServiceImpl.java` — implementação com métodos `private`
