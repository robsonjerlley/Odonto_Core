# ADR-023: TicketWonEvent — Contrato do Evento de Fechamento

**Status**: **Substituída pela ADR-029** (2026-06-28)
**Data**: 2026-06-17
**Autores**: Arquiteto-Agent
**Impacto**: `LeadTicketServiceImpl`, Módulo Financeiro, Módulo Consultas, `AnalyticsServiceImpl`
**Relaciona**: ADR-022 (clinicId foundation), ADR-024 (enforcement `@TenantId` + `TenantContext`), ADR-015 (analytics scope-aware), ADR-020 (Virtual Threads), ADR-003 (imutabilidade)
**Pré-requisito**: ADR-022 implementado + **ADR-024 implementada** (`TenantContext` + `@TenantId`)

---

> # 🎯 NÃO IMPLEMENTAR ESTA ADR — substituída pela [ADR-029](ADR-029-scheduling-agenda-evaluator-deal-snapshot.md)
>
> O gatilho de fechamento → módulo downstream foi **fechado de forma diferente** na ADR-029 e é
> esse o contrato vigente. O que mudou:
>
> | Dimensão | ADR-023 (esta — **morta**) | ADR-029 (vigente) |
> |---|---|---|
> | Evento | `TicketWonEvent` | **`DealWonEvent`** |
> | Publicado em | `LeadTicketServiceImpl.updateStatus` (WIN) | **`DealServiceImpl.closeDeal`** |
> | Mecanismo | `@Async` + `AFTER_COMMIT` | **síncrono, mesma transação (fail-fast)** |
> | Consumidores | `FinancialEventListener` + `ClinicalEventListener` (módulos Financeiro/Consultas) | **`AppointmentEventListener`** (módulo `appointment`) |
> | `TenantContext.set/clear` no listener | obrigatório (thread async) | **dispensável** (síncrono, tenant já no contexto) |
>
> Os módulos "Financeiro" e "Consultas" que esta ADR pressupõe **não foram criados** — o pedaço de
> pagamento virou a [ADR-031](ADR-031-commercial-deal-payment-status.md) (`Deal.paymentStatus`, sem
> evento), e a agenda virou o módulo `appointment` (ADR-029). Se um dia um módulo financeiro real
> exigir entrega assíncrona pós-commit, **abrir uma ADR nova** — o padrão async + `TenantContext`
> descrito abaixo continua sendo a referência técnica correta para esse caso, mas o contrato
> `TicketWonEvent` em si está descontinuado.
>
> 📜 Tudo abaixo é **histórico** — preservado pelo raciocínio de design (event-carried state transfer,
> AFTER_COMMIT, executor dedicado), não como instrução de implementação.

---

> ## ⚠️ Revisão 2026-06-22 — listeners async e o `TenantContext` (ADR-024)
>
> Com a adoção do `@TenantId` (ADR-024), o `clinicId` das entidades dos módulos downstream
> passa a ser preenchido **automaticamente pelo Hibernate** — desde que o tenant esteja no
> `TenantContext`. Mas os listeners desta ADR rodam em `@Async` (outra thread) **após o commit**,
> onde o `SecurityContext` **não existe**. Sem ação, o resolver retorna `null` e as entidades
> nascem com `clinicId` nulo — o bug que a §4 jura proibir.
>
> **Correção mandatória** (detalhada na seção 3 abaixo): cada listener deve fazer
> `TenantContext.set(event.clinicId())` no início e `TenantContext.clear()` em `finally`, usando
> o `clinicId` que **já viaja no payload** do `TicketWonEvent` (§1). Consequência positiva: a
> regra da §4 (`setClinicId` manual) **deixa de ser necessária** — vira comportamento do ORM;
> resta apenas garantir o `TenantContext` no listener.

---

## Contexto

A transição `TicketStatus → WIN` em `LeadTicketServiceImpl` é hoje o terminal de sucesso da esteira. Nenhum módulo reage a esse evento — o fluxo simplesmente encerra no WIN.

Os módulos Financeiro e de Consultas dependem desse evento como gatilho de inicialização. A questão de design é: como o `LeadTicketServiceImpl` comunica o WIN sem conhecer os módulos downstream?

Chamada direta (`financialService.create()` dentro de `updateStatus()`) acopla os módulos e faz com que falha no financeiro reverta o WIN — comportamento incorreto. O WIN é um fato do funil; o que acontece depois é responsabilidade de cada módulo downstream.

Esta ADR define:

1. O contrato imutável do `TicketWonEvent`
2. O mecanismo de publicação e consumo
3. As regras de design obrigatórias para entidades e analytics dos módulos downstream

---

## Decisão

### 1. `TicketWonEvent` — contrato do evento

```java
// pacote: io.sertaoBit.odontocore.crm.core.events

public record TicketWonEvent(
    UUID ticketId,
    UUID clinicId,           // obrigatório — ADR-022
    UUID customerId,
    UUID dealId,
    UUID closedBy,
    LocalDateTime closedAt,
    DealSnapshot deal        // snapshot imutável do Deal no momento do WIN
) {}
```

`DealSnapshot` é um value object carregado no evento. Os módulos downstream recebem tudo que precisam no payload — sem query adicional ao banco do módulo `commercial`:

```java
public record DealSnapshot(
    BigDecimal finalValue,
    PaymentMethod paymentMethod,
    List<DealProcedureSnapshot> procedures
) {}

public record DealProcedureSnapshot(
    String name,
    String code,
    BigDecimal tableValue,
    int quantity
) {}
```

`DealSnapshot.from(deal)` é um factory method estático em `DealSnapshot`, responsável por construir o snapshot a partir da entidade `Deal`. O `Deal` é carregado antes da publicação (já necessário para o fluxo de `closeDeal()`).

---

### 2. Publicação — `LeadTicketServiceImpl`

Dentro de `updateStatus()`, após salvar o ticket com status WIN e ainda dentro do `@Transactional`:

```java
if (newStatus == TicketStatus.WIN) {
    applicationEventPublisher.publishEvent(
        new TicketWonEvent(
            ticket.getId(),
            currentUser.getClinicId(),
            ticket.getCustomerId(),
            deal.getId(),
            currentUser.getId(),
            ticket.getClosedAt(),
            DealSnapshot.from(deal)
        )
    );
}
```

O evento é publicado dentro da transação. A entrega ao listener só ocorre após o COMMIT (ver item 3).

---

### 3. Consumo — `@TransactionalEventListener(AFTER_COMMIT)`

Cada módulo downstream implementa seu próprio listener desacoplado. **⚠️ Obrigatório (ADR-024)**: estabelecer o `TenantContext` a partir de `event.clinicId()` antes de chamar o service, e limpá-lo em `finally` — a thread `@Async` não herda o `SecurityContext` e a thread retorna ao pool:

```java
// FinancialEventListener.java
@Component
public class FinancialEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("tenantAwareExecutor")          // executor dedicado — ver nota abaixo
    public void onTicketWon(TicketWonEvent event) {
        TenantContext.set(event.clinicId());   // ← tenant do PAYLOAD, não do SecurityContext
        try {
            financialService.initializeFromWin(event);   // @TenantId preenche clinicId sozinho
        } finally {
            TenantContext.clear();             // ← obrigatório: thread volta limpa ao pool
        }
    }
}

// ClinicalEventListener.java
@Component
public class ClinicalEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("tenantAwareExecutor")
    public void onTicketWon(TicketWonEvent event) {
        TenantContext.set(event.clinicId());
        try {
            clinicalService.initializeFromWin(event);
        } finally {
            TenantContext.clear();
        }
    }
}
```

> 📐 **Ordem importa**: `TenantContext.set()` precisa rodar **antes** de `initializeFromWin()` abrir
> sua transação `@Transactional` — é na abertura da sessão Hibernate que o `@TenantId` consulta o
> resolver (ADR-024 §6). Como a transação do funil já commitou (AFTER_COMMIT), a do listener é nova.

**Por que `AFTER_COMMIT`**: garante que o listener só processa se a transação do funil foi comitada com sucesso. Falha no WIN antes do commit = evento nunca entregue. Comportamento correto: os módulos downstream só reagem a WINs durável no banco.

**Por que `@Async`**: o processamento dos módulos não ocorre na thread de request do funil. Virtual Threads (ADR-020) gerenciam concorrência no Tomcat; `@Async` aqui garante separação de ciclo de vida entre o request de WIN e o processamento downstream.

**Por que executor dedicado (`tenantAwareExecutor`)**: além de evitar contenção no pico (risco 2 abaixo), um executor próprio isola o ciclo de vida do `TenantContext` desses listeners — nenhuma thread compartilhada com outro fluxo que possa observar um `TenantContext` não limpo. Definir `@Bean ThreadPoolTaskExecutor tenantAwareExecutor` (ou virtual-thread executor por task, alinhado à ADR-020). O `clear()` em `finally` continua obrigatório independentemente do executor.

**Falha no listener**: não reverte o WIN. O ticket permanece WIN. Os listeners devem:
- Logar a falha de forma estruturada (ticketId, clinicId, erro)
- Implementar lógica de reconciliação ou alertar operação — fora do escopo desta ADR

---

### 4. Regra mandatória para entidades dos módulos downstream

Toda entidade criada pelos módulos Financeiro e Consultas deve ter:

```java
@TenantId                              // ADR-024 — Hibernate preenche e filtra automaticamente
@Column(name = "clinic_id", nullable = false, updatable = false)
private UUID clinicId;                 // derivado do TenantContext (set do TicketWonEvent.clinicId)

@Column(nullable = false, updatable = false)
private UUID ticketId;                 // rastreabilidade até o ticket de origem

@CreationTimestamp
private LocalDateTime createdAt;       // imutável (sem update)
```

Sem exceções. Entidade sem `clinicId` = bug de isolamento de tenant. Entidade sem `ticketId` = rastreabilidade quebrada.

> 🔄 **Mudança vs. versão original (ADR-024)**: o `clinicId` **não é mais setado manualmente**
> (`record.setClinicId(event.clinicId())`). Com `@TenantId`, o Hibernate o preenche a partir do
> `TenantContext` — que o listener populou com `event.clinicId()` (§3). O `ticketId`, por não ser
> tenant, **continua sendo setado explicitamente** a partir do evento. Não confundir os dois:
> `clinicId` = automático via `@TenantId`; `ticketId` = manual via payload.

---

### 5. Analytics — regra para métodos novos

Todo método novo em `AnalyticsServiceImpl` que consome dados dos módulos Financeiro ou Consultas deve:

1. Receber `clinicId` como parâmetro (extraído de `currentUser.getClinicId()` no controller)
2. Aplicar `clinicId` como filtro obrigatório na query (`WHERE clinic_id = ?`)
3. Incluir `clinicId` na chave de cache Redis

```java
// exemplo de chave de cache com clinicId
@Cacheable(
    value = "analytics",
    key = "'revenue:' + #clinicId + ':' + #from + ':' + #to"
)
public RevenueResultDTO getRevenueRealized(UUID clinicId, LocalDate from, LocalDate to) {
    // query com WHERE clinic_id = :clinicId obrigatório
}
```

O `clinicId` é o primeiro segmento da chave depois do namespace — permite evicção cirúrgica por clínica (`analytics:{clinicId}:*`) sem afetar outras clínicas.

---

## Diagrama do fluxo

```
[LeadTicketServiceImpl.updateStatus() → WIN]
    ↓ @Transactional (ainda aberta)
    ↓ publica TicketWonEvent (com DealSnapshot + clinicId)
    ↓ COMMIT

    ↓ AFTER_COMMIT dispara (fora da transação do funil)
    ├── [FinancialEventListener] @Async
    │       → financialService.initializeFromWin(event)
    │       → cria FinancialRecord { clinicId, ticketId, ... }
    │       → cria PaymentSchedule { clinicId, ticketId, ... }
    │
    └── [ClinicalEventListener] @Async
            → clinicalService.initializeFromWin(event)
            → cria TreatmentPlan { clinicId, ticketId, ... }
            → cria Consultation  { clinicId, ticketId, ... }

[Analytics — métodos novos]
    → lê FinancialRecord WHERE clinic_id = :clinicId
    → lê Consultation    WHERE clinic_id = :clinicId
    → cache key: "analytics:{clinicId}:revenue:..."
```

---

## Consequências positivas

- `LeadTicketServiceImpl` não conhece Financeiro nem Consultas — acoplamento zero entre módulos.
- Módulos downstream nascem multi-tenant: `clinicId` em todos os dados desde o primeiro registro.
- `DealSnapshot` no payload elimina query cross-module: os módulos downstream não dependem do repositório de `Deal`.
- `AFTER_COMMIT` garante consistência: listeners processam apenas WINs durável.
- Analytics novos entram com filtro de tenant e chave de cache corretos desde o início.
- Futura extração para microsserviços: o contrato do evento é o mesmo — apenas o mecanismo de transporte muda (Spring Events → Kafka/RabbitMQ).

## Consequências negativas / riscos

| Risco | Mitigação |
|---|---|
| Listener falha após commit (WIN no DB, FinancialRecord não criado) | Log estruturado obrigatório no catch; reconciliação por query `tickets WIN sem financial_record` pode ser adicionada como job noturno. ⚠️ Esse job é cross-tenant e roda sem request — deve **iterar por clínica** com `TenantContext.set(clinicId)` → query → `clear()` (ADR-024 §6), pois o `@TenantId` filtra sempre |
| `TenantContext` não limpo no listener vaza tenant para a próxima task do pool | `try/finally TenantContext.clear()` obrigatório (§3) + executor dedicado `tenantAwareExecutor` |
| `@Async` com pool de threads compartilhado pode gerar contenção em pico | Definir executor dedicado para listeners (`@Bean ThreadPoolTaskExecutor listenerExecutor`) se houver gargalo monitorado |
| `DealSnapshot` desatualizado se Deal sofrer correção após WIN | `Deal.archived = true` após WIN por regra de negócio — snapshot é consistente com o estado no momento do fechamento |

---

## Alternativas descartadas

- **Chamada direta de service** (`financialService.create()` dentro de `updateStatus()`): acopla módulos; falha no financeiro reverte o WIN. Descartado.
- **Mensageria externa** (Kafka, RabbitMQ): overhead operacional desnecessário para o porte atual no Railway. Spring Application Events resolvem o desacoplamento no monolito com custo zero de infra. O contrato do `TicketWonEvent` (record) é agnóstico ao transporte — pode ser serializado para Kafka sem mudança de contrato quando necessário.
- **Polling** (`@Scheduled` busca tickets WIN sem FinancialRecord): latência de até 1 ciclo do job, complexidade de rastreamento de estado processado, e impossibilidade de carregar `DealSnapshot` sem nova query. Descartado.

---

## Ordem de implementação

```
0. [PRÉ-REQUISITO] ADR-024 implementada      → TenantContext + ClinicTenantResolver + @TenantId
1. core/events/TicketWonEvent.java          → record + DealSnapshot + DealProcedureSnapshot
2. LeadTicketServiceImpl.java               → publicar evento no bloco WIN
3. Módulo Financeiro:
   a. FinancialRecord.java                  → entidade (@TenantId no clinicId + ticketId manual)
   b. PaymentSchedule.java                  → entidade (@TenantId no clinicId + ticketId manual)
   c. FinancialService / Impl               → initializeFromWin(TicketWonEvent)
   d. FinancialEventListener.java           → @TransactionalEventListener + @Async + TenantContext set/clear
4. Módulo Consultas:
   a. TreatmentPlan.java / Consultation.java → entidades (@TenantId no clinicId + ticketId manual)
   b. ClinicalService / Impl                → initializeFromWin(TicketWonEvent)
   c. ClinicalEventListener.java            → @TransactionalEventListener + @Async + TenantContext set/clear
5. config — @Bean tenantAwareExecutor       → executor dedicado dos listeners (ADR-020/ADR-024)
6. AnalyticsServiceImpl.java                → novos métodos com clinicId explícito + cache key correta
```

---

## Referências

- ADR-022 — clinicId em User + JWT (fundação)
- ADR-024 — enforcement `@TenantId` + `TenantContext` (pré-requisito: `clinicId` automático nas entidades; listeners populam o `TenantContext` do payload)
- ADR-025 — RLS PostgreSQL (defesa em profundidade futura)
- ADR-015 — analytics scope-aware (padrão de `checkOrThrow` / `getScope()` para novos endpoints)
- ADR-020 — Virtual Threads (contexto de `@Async` e concorrência no Tomcat)
- ADR-003 — ContactLog imutabilidade (mesmo princípio aplicado a eventos e registros financeiros)
- spec-redis-cache.md — chaves de cache para analytics novos devem seguir padrão `{clinicId}:...`
