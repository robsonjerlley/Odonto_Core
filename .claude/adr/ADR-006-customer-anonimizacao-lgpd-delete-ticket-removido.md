# ADR-006: Anonimização de PII em Customer (LGPD) e remoção do endpoint DELETE de LeadTicket

**Status**: Aceito  
**Data**: 2026-06-01  
**Autores**: Arquiteto-Agent  
**Impacto**: Módulo funnel — CustomerController, CustomerService, CustomerServiceImpl, LeadTicketController, LeadTicketService, LeadTicketServiceImpl

---

## Contexto

### Situação atual

`DELETE /api/v1/customers/{id}` e `DELETE /api/v1/tickets/{id}` executam deleção física (`deleteById`) no banco. Isso cria dois problemas independentes:

**Problema 1 — LGPD e módulos futuros em conflito**

O CRM precisa honrar o direito ao esquecimento (LGPD Art. 18, VI) quando um cliente rompe contato definitivamente e solicita a remoção dos seus dados. A deleção física resolve o requisito de privacidade, mas destrói a integridade referencial: ContactLogs, LeadTickets, Deals e todas as métricas de analytics referenciam `customerId` (UUID). A remoção da linha em `customers` torna essas referências inválidas e inutiliza o histórico financeiro e operacional da clínica.

Os módulos de agendamento (consultas) e financeiro, previstos para fases posteriores ao MVP, precisarão referenciar o mesmo `customerId` UUID sem acesso a PII. A deleção física impossibilita esse reaproveitamento.

**Problema 2 — Endpoint DELETE em LeadTicket não tem caso de negócio**

O ciclo de vida de um LeadTicket é inteiramente gerenciado pela máquina de estados via `changeStatus()`. Os estados terminais são `LOSS`, `WIN` e `POST_PROCEDURE → LOSS`. Não existe cenário de negócio legítimo para remover um ticket: fazê-lo apaga ContactLogs associados, quebra o `previousTicketId` do ticket filho (gerado pelo RecycleJob) e corrompe as métricas do módulo analytics.

A situação é análoga à de `ContactLog` (ADR-003): o endpoint existe mas nunca deveria ter sido criado.

---

## Decisão

### `DELETE /customers/{id}` — comportamento muda de deleção para anonimização

O endpoint é mantido com a mesma URL e método HTTP. O que muda é a implementação do service: em vez de `customerRepository.deleteById(id)`, o método `anonymize(UUID id)` substitui os campos PII por valores neutros e persiste a linha.

**Campos anonimizados (PII — dados pessoais identificáveis):**

| Campo | Valor após anonimização |
|---|---|
| `name` | `"CLIENTE ANONIMIZADO"` |
| `cpf` | `null` |
| `phone` | `null` |
| `phone2` | `null` |
| `email` | `null` |
| `initialNote` | `null` |
| `adCampaign` | `null` |
| `referredBy` | `null` |

**Campos preservados (dados de negócio, não PII):**

| Campo | Motivo |
|---|---|
| `id` (UUID) | Chave referencial — sem ele, ContactLogs, Tickets, Deals ficam órfãos |
| `source` | Dado operacional para métricas de ROI de canal |
| `adChannel` | Dado operacional para métricas de ROI de ADS |
| `createdAt`, `updatedAt`, `createdBy` | Auditoria de quem criou o registro — não identifica o cliente |

O UUID do cliente anonimizado permanece válido. Todos os módulos que o referenciam continuam operacionais. O cliente, como pessoa identificável, deixa de existir no sistema.

**Assinatura do método no service:**

```
CustomerService:     void anonymize(UUID id, UUID currentUserId)
CustomerServiceImpl: implementa anonymize() com checkOrThrow(DELETE) + persist
```

O método `deleteById()` é removido da interface e da implementação.

---

### `DELETE /api/v1/tickets/{id}` — endpoint removido

- `LeadTicketController`: remover o método `delete()` e a anotação `@DeleteMapping("/{id}")`
- `LeadTicketService`: remover a declaração `void deleteById(UUID id)` da interface
- `LeadTicketServiceImpl`: remover a implementação do método `deleteById(UUID id)`
- `PermissionSeeder`: nenhuma regra `DELETE` para `Resource.TICKET` será adicionada agora nem no futuro sem nova ADR explícita

O ciclo de vida de um ticket termina em `LOSS` ou via `WIN → POST_PROCEDURE → LOSS`. Não há estado "deletado".

---

## Consequências Positivas

- LGPD cumprida: os dados pessoais do cliente são eliminados a pedido, sem violar referências existentes
- Integridade referencial preservada: Deals, ContactLogs, analytics e histórico financeiro permanecem intactos após anonimização
- Módulos futuros (agendamento, financeiro) podem referenciar o UUID sem precisar de PII
- Métricas de ROI e conversão do módulo analytics não são corrompidas por ausência de registro
- Endpoint `DELETE /customers/{id}` mantém o contrato HTTP — o chamador não precisa saber que é anonimização, não deleção
- Remoção do DELETE de LeadTicket protege a integridade da esteira e do histórico de auditoria — consistente com ADR-003 (ContactLog)

## Consequências Negativas / Riscos

- O registro anonimizado ocupa espaço em banco indefinidamente. Mitigação: volume de clientes de uma clínica odontológica é baixo — não é problema operacional relevante no horizonte do MVP
- `GET /customers/{id}` retorna o registro anonimizado com `name = "CLIENTE ANONIMIZADO"`. O frontend precisa tratar esse estado visualmente. Mitigação: adicionar campo `anonymized: boolean` ao `CustomerResponseDTO` para que o frontend possa distinguir o estado sem precisar inspecionar o nome
- A constraint `UNIQUE` em `cpf` pode ser problemática após anonimização se múltiplos clientes forem anonimizados (todos teriam `cpf = null`). Mitigação: `null` não viola `UNIQUE` no PostgreSQL — múltiplos `null` são permitidos na mesma coluna com constraint `UNIQUE`

---

## Alternativas Consideradas

- **Hard delete com cascade**: descartado. Destrói ContactLogs, compromete analytics, impossibilita reaproveitamento do UUID pelos módulos futuros. Resolve LGPD e quebra tudo o mais.
- **Soft delete com `active = false`**: descartado. O dado PII permanece em banco — não satisfaz LGPD. Seria compliance de fachada.
- **Soft delete com `deletedAt` timestamp**: descartado pelo mesmo motivo. O dado existe enquanto a linha existir, independentemente de flags.
- **Manter DELETE em LeadTicket com RBAC restritíssimo (apenas ADM_SYSTEM)**: descartado. Não há caso de uso legítimo. Manter o endpoint é débito sem retorno e risco de corrupção de dados por uso acidental.

---

## Referências Cruzadas

- `ADR-003` — remoção do endpoint DELETE de ContactLog; mesmo princípio aplicado a LeadTicket
- `ADR-004` — padrão `checkOrThrow(DELETE)` a ser aplicado no método `anonymize()` antes da operação
- `PermissionSeeder.java` — a permissão `Action.DELETE` para `Resource.CUSTOMER` continua válida; mapeia para a operação de anonimização
- `CustomerResponseDTO` — adicionar campo `anonymized: boolean` para sinalização ao frontend