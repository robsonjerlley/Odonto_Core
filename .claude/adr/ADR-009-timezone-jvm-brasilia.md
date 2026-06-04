# ADR-009: Fuso horário fixo (America/Sao_Paulo) na JVM

**Status**: Aceito
**Data**: 2026-06-04
**Autores**: Claude (alinhamento frontend ↔ backend)
**Impacto**: `Application`, `application.properties`, `commercial/service/impl/RecycleJob`, todos os campos `LocalDateTime`/`LocalDate` gerados via `now()` e `@CreationTimestamp`/`@UpdateTimestamp`

---

## Contexto

### Problema identificado

O backend **não fixava o fuso horário**. `LocalDateTime.now()`, `LocalDate.now()`, os timestamps do Hibernate (`@CreationTimestamp`/`@UpdateTimestamp`) e o cron do `RecycleJob` usavam o fuso **padrão da JVM**. Em contêiner (Railway/Docker) a JVM sobe em **UTC**, enquanto a clínica e o frontend operam em **horário de Brasília (UTC-3)**.

Consequências concretas:

1. **Divergência de 3 horas**: um agendamento gravado às 14h (Brasília) pelo frontend convivia com `createdAt`/`closedAt` gerados pelo backend em UTC (17h). A mesma linha do banco misturava dois fusos.
2. **`LocalDateTime` não carrega offset**: o frontend recebe `"2026-06-04T17:00:00"` sem saber se é UTC ou local — impossível converter com segurança na exibição.
3. **Cron ambíguo**: `@Scheduled(cron = "0 0 2 * * *")` rodava às 02:00 UTC (23:00 de Brasília do dia anterior), não às 02:00 locais como pretendido.

### Por que `spring.jackson.time-zone` sozinho não resolve

`spring.jackson.time-zone` afeta a serialização de tipos **com zona** (`Instant`, `Date`, `ZonedDateTime`). **`LocalDateTime` é serializado como está, sem conversão** — então a propriedade não corrige os campos que o domínio realmente usa. O ponto de origem do problema é o `now()`, que depende do fuso **default da JVM**, não do Jackson.

---

## Decisão

### Fixar o fuso default da JVM em `Application.main`, antes do contexto Spring subir

```java
String timezone = System.getenv().getOrDefault("APP_TIMEZONE", "America/Sao_Paulo");
TimeZone.setDefault(TimeZone.getTimeZone(timezone));
SpringApplication.run(Application.class, args);
```

Definir antes de `SpringApplication.run` garante que **Hibernate e o scheduler já inicializem com o fuso correto**. Com isso:

- `LocalDateTime.now()` / `LocalDate.now()` → horário de Brasília
- `@CreationTimestamp` / `@UpdateTimestamp` → horário de Brasília
- O cron do `RecycleJob` → 02:00 de Brasília

Configurável via env `APP_TIMEZONE` (default `America/Sao_Paulo`) — permite ajuste sem rebuild se a clínica mudar de região.

### Reforços complementares

1. **`spring.jackson.time-zone=America/Sao_Paulo`** em `application.properties` — garante consistência para qualquer tipo com zona que venha a ser introduzido (não afeta `LocalDateTime`, mas remove ambiguidade futura).
2. **`@Scheduled(cron = "0 0 2 * * *", zone = "America/Sao_Paulo")`** no `RecycleJob` — torna o horário do job explícito no código, independente do fuso da JVM (cinto e suspensório).

### Alinhamento com o frontend

O frontend **já** envia e exibe horário de Brasília de forma "naïve" (`nowBrasiliaISO` em `utils.ts`, sem offset). Com o backend agora também em Brasília, **ambos os lados ficam no mesmo fuso** — ida e volta consistentes, sem skew. **Nenhuma conversão UTC→local é necessária no frontend.**

> A recomendação anterior do contrato de integração (§1, "tratar `LocalDateTime` recebido como UTC") fica **revogada** por esta ADR: tratá-lo como UTC, agora que o backend grava em Brasília, reintroduziria um erro de 3h. O `LocalDateTime` deve ser exibido como horário local naïve.

---

## Arquivos alterados

```
MODIFICADOS (3 arquivos)
├── Application.java                                   (TimeZone.setDefault antes do run)
├── src/main/resources/application.properties          (spring.jackson.time-zone)
└── commercial/service/impl/RecycleJob.java            (zone explícito no @Scheduled)

BANCO DE DADOS
└── Nenhuma migration. Dados históricos em UTC (se houver, de ambiente de teste)
   não são reconvertidos — feature ainda não em produção com volume real.
```

---

## Consequências Positivas

- Backend e frontend no mesmo fuso — fim do skew de 3h em timestamps
- `LocalDateTime` exibível diretamente como hora local, sem heurística de conversão
- Cron do RecycleJob roda no horário pretendido (02:00 local)
- Comportamento idêntico em dev (Windows local, já em Brasília) e produção (contêiner UTC)

## Consequências Negativas / Riscos

- **`TimeZone.setDefault` é global ao processo**: afeta toda a JVM. Aceitável — a aplicação é monolítica e single-tenant por clínica; não há requisito multi-fuso.
- **Persistência sem offset permanece**: se um dia houver operação multi-região, será preciso migrar para `Instant`/`timestamptz`. Fora do escopo do MVP.
- Horário de verão: o Brasil não adota DST desde 2019; `America/Sao_Paulo` lida corretamente caso volte.

---

## Alternativas Consideradas

- **Só `spring.jackson.time-zone`**: descartado — não afeta `LocalDateTime`, que é o tipo usado em todo o domínio.
- **Variável `TZ` no contêiner Railway**: funciona, mas fica fora do versionamento e some se o serviço for recriado sem a env. A fixação em código é o default seguro; `APP_TIMEZONE` ainda permite override por env.
- **Migrar todo o domínio para `Instant`/`OffsetDateTime`**: correto a longo prazo, mas é refactor amplo (entidades, DTOs, frontend) desproporcional ao MVP. Registrado como evolução futura.
- **`-Duser.timezone=America/Sao_Paulo` no comando de start**: equivalente, mas depende do script de deploy. Fixar em `main` cobre qualquer forma de execução (IDE, jar, contêiner).

---

## Referências Cruzadas

- `frontend-integration-contract.md` §1 (Timezone) — recomendação de tratar como UTC fica revogada por esta ADR; atualizar a seção 15 (LACUNAS) marcando o item como resolvido
- Frontend `lib/utils.ts` (`nowBrasiliaISO`) — origem do horário de Brasília no envio; permanece válido e agora simétrico ao backend
