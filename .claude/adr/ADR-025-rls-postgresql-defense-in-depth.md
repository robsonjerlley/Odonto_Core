# ADR-025: Row Level Security (PostgreSQL) — Defesa em Profundidade

**Status**: Proposto (implementação futura — não agendada)
**Data**: 2026-06-22
**Autores**: Arquiteto-Agent
**Impacto (futuro)**: todas as tabelas multi-tenant, conexão JDBC/HikariCP, camada transacional, migrations Flyway, provisionamento Railway
**Relaciona**: ADR-024 (`@TenantId` — camada de aplicação que esta ADR complementa), ADR-022 (fundação clinicId)
**Pré-requisito**: ADR-024 implementada e estável; segunda clínica real em produção (ou exigência de compliance sobre dados clínicos/financeiros)

---

## Contexto

A ADR-024 garante o isolamento de tenant na **camada de aplicação**: o Hibernate (`@TenantId`) injeta `WHERE clinic_id = ?` em toda query gerada pelo ORM. Isso é suficiente para o porte atual.

Porém, esse isolamento tem duas fronteiras que a aplicação não cobre:

1. **Queries que escapam do ORM**: `@Query(nativeQuery = true)`, scripts de manutenção, acesso direto via `psql`, ferramentas de BI conectadas ao banco, ou uma futura segunda aplicação sobre o mesmo schema.
2. **Bug na própria aplicação**: se um `TenantContext.set()` for esquecido ou um resolver retornar o tenant errado, o `@TenantId` confia nesse valor — não há rede de proteção abaixo dele.

**Row Level Security (RLS)** desce o isolamento para a camada do **banco**. O PostgreSQL passa a recusar fisicamente linhas de outra clínica, independentemente de quem ou o quê emitiu a query. É *defesa em profundidade*: a aplicação erra, o banco segura.

> 📐 Esta ADR **não substitui** a ADR-024 — as duas camadas coexistem. `@TenantId` é o isolamento primário (ergonômico, no código); RLS é a rede de segurança (no dado).

---

## Por que NÃO implementar agora

| Motivo | Detalhe |
|---|---|
| Risco atual é zero | Uma única clínica em produção; não há dado de outra clínica para vazar |
| Atrito com HikariCP/Railway | RLS exige disciplina de `SET LOCAL` por transação em pool reutilizado — fonte conhecida de bug neste projeto |
| Gestão de role no Railway | Exige role de aplicação **não-owner** ou `FORCE ROW LEVEL SECURITY` — provisionamento extra |
| Custo de migration | `ENABLE` + `FORCE` + `CREATE POLICY` por tabela, mais manutenção contínua |

🎯 **Recomendação**: manter como ADR **Proposta**. Ativar quando entrar a segunda clínica real **ou** quando compliance (dados clínicos/financeiros de múltiplos tenants) exigir garantia no nível do dado. Até lá, ADR-024 basta.

---

## Decisão (quando ativada)

### 1. Policy por tabela multi-tenant

```sql
-- migration Flyway, por tabela: customers, lead_tickets, deals, financial_record, etc.
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers FORCE  ROW LEVEL SECURITY;   -- ⚠️ ver §3 (owner bypass)

CREATE POLICY clinic_isolation ON customers
  USING      (clinic_id = current_setting('app.clinic_id')::uuid)   -- filtra leitura/update/delete
  WITH CHECK (clinic_id = current_setting('app.clinic_id')::uuid);  -- valida insert/update
```

- `USING` → o que **sai** (SELECT/UPDATE/DELETE só enxergam a clínica do contexto).
- `WITH CHECK` → o que **entra** (impede INSERT/UPDATE gravar linha de outra clínica).

### 2. Definição da variável de sessão — dentro da transação

A variável `app.clinic_id` deve ser definida **na mesma transação** das queries de negócio, com bind parameter (nunca concatenação — risco de injection) e em escopo `LOCAL`:

```java
// executado no início da transação, antes das queries de negócio
entityManager.createNativeQuery(
        "SELECT set_config('app.clinic_id', :cid, true)")  // true = LOCAL à transação
    .setParameter("cid", TenantContext.get().toString())
    .getSingleResult();
```

> 🚫 **Anti-pattern crítico — `SET` global**: usar `SET app.clinic_id` (sem `LOCAL`) ou `set_config(..., false)` faz o valor **persistir na conexão física** do HikariCP e vazar para a próxima request que pegar aquela conexão do pool. **Sempre** `LOCAL`/`true`. O valor morre no COMMIT/ROLLBACK.

> ⚠️ **Timing vs. transação**: um `HandlerInterceptor.preHandle()` roda **antes** da transação `@Transactional` abrir — definir a variável lá não funciona. O `set_config` precisa participar da transação de negócio. Padrões viáveis: um aspecto (`@Around` em `@Transactional`), um `TransactionSynchronization`, ou um wrapper de repositório. Decidir o mecanismo na implementação.

### 3. Role de aplicação não pode ser owner

🚫 **Armadilha de produção**: o owner do banco **ignora RLS por padrão**. No Railway a aplicação costuma conectar como owner — a policy não faria efeito e o vazamento passaria despercebido até produção. Duas saídas:

- `ALTER TABLE ... FORCE ROW LEVEL SECURITY` (aplica RLS inclusive ao owner); **ou**
- criar um role dedicado não-owner para a aplicação (mais limpo, exige provisionamento no Railway).

### 4. Interação com `@TenantId` (ADR-024)

As duas camadas usam a mesma fonte (`TenantContext`):

```
TenantContext.get()
   ├── ADR-024: alimenta o ClinicTenantResolver (Hibernate filtra) ── isolamento primário
   └── ADR-025: alimenta set_config('app.clinic_id') (Postgres filtra) ── rede de segurança
```

O `@TenantId` continua sendo a ergonomia do dia a dia; a RLS apenas garante que, se ele falhar, o banco recusa. Não há conflito: ambos filtram pelo mesmo `clinic_id`.

---

## Arquivos atingidos (futuro)

| Arquivo / Artefato | Mudança |
|---|---|
| `config/tenant/` (novo aspecto ou `TransactionSynchronization`) | Emitir `set_config('app.clinic_id', ...)` por transação |
| Migrations Flyway (uma por tabela) | `ENABLE` + `FORCE` RLS + `CREATE POLICY` |
| Provisionamento Railway | Role de aplicação não-owner (se escolhido em vez de `FORCE`) |
| `application.properties` | Eventual datasource com role dedicado |
| Testes de integração | IT confirmando que `psql`/query nativa sem `set_config` não retorna nada |

> O `TenantContext` (ADR-024) é reaproveitado integralmente — esta ADR não cria nova fonte de tenant.

---

## Consequências positivas

- Isolamento garantido na camada do dado: query nativa, BI, `psql` ou bug de aplicação não vazam.
- Fail-safe real: na ausência de `app.clinic_id`, a policy retorna **zero linhas** (não dados de outra clínica).
- Reutiliza o `TenantContext` da ADR-024 — sem nova fonte de verdade.

## Consequências negativas / riscos

| Risco | Mitigação |
|---|---|
| `SET` sem `LOCAL` vaza tenant pela conexão do pool | Sempre `set_config(..., true)`; teste de pool com requisições alternadas entre clínicas |
| `set_config` fora da transação não tem efeito | Mecanismo transacional (aspecto/synchronization), nunca interceptor pré-transação |
| Owner do banco bypassa RLS silenciosamente | `FORCE ROW LEVEL SECURITY` ou role não-owner — validar em IT |
| Overhead de manutenção por tabela | Template de migration padronizado; checklist de nova tabela |
| Operações cross-tenant legítimas bloqueadas | Role administrativo separado ou `set_config` com sentinela; auditado |

---

## Alternativas consideradas

- **Apenas ADR-024 (`@TenantId`), sem RLS**: é o estado atual aceito. Suficiente enquanto o risco for baixo. Esta ADR é o gatilho documentado para quando deixar de ser.
- **RLS substituindo `@TenantId`**: descartado — RLS no banco é rede de segurança, não ergonomia. Sem o `@TenantId`, todo dev escreveria `set_config` manualmente; pior DX e mesmo risco de esquecimento que a ADR-022 original.
- **Schema por tenant**: já descartado na ADR-022 (operação complexa no Railway).

---

## Gatilho de ativação (checklist)

Reabrir esta ADR e mover para **Aceito** quando **qualquer** condição for verdadeira:

- [ ] Segunda clínica real provisionada em produção
- [ ] Exigência de compliance sobre isolamento de dados clínicos/financeiros entre tenants
- [ ] Conexão de ferramenta externa (BI, relatórios) direto ao banco de produção
- [ ] Segunda aplicação compartilhando o schema

---

## Referências

- ADR-024 — `@TenantId` + `TenantContext` (isolamento primário; esta ADR complementa)
- ADR-022 — fundação clinicId
- [PostgreSQL — Row Security Policies](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [PostgreSQL — `set_config()` e `SET LOCAL`](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADMIN-SET)
