''''''''''# ADR-027: Correções de boot — schema, Flyway (PG18), sentinela de tenant e seed do admin

**Status**: Implementado
**Data**: 2026-06-23
**Autores**: Arquiteto-Agent
**Impacto**: `application.properties`, `application-local.properties`, `application-prod.properties`, `db/migration/V1`, `ClinicResolveTenant`, `UserSeeder`
**Relaciona**: ADR-024 (`@TenantId` + `TenantContext` — esta ADR corrige o boot da fundação), ADR-022 (clínica única), ADR-014 (Flyway)

---

## Contexto

Após o `drop`/recriação do banco local (apenas os schemas `crm_db` e `identity_db` recriados, sem tabelas), a aplicação não subia. A investigação revelou **quatro falhas encadeadas** — cada uma escondia a próxima no boot:

1. `Schema validation: missing column [clinic_id] in table [contact_logs]`
2. `Unsupported Database: PostgreSQL 18.3` (Flyway)
3. `SessionFactory configured for multi-tenancy, but no tenant identifier specified`
4. `Could not resolve placeholder 'app.admin.clinicId'`

A causa comum: a fundação multi-tenant da ADR-024 foi implementada, mas o **boot em banco limpo** nunca havia sido exercitado ponta a ponta. Cada peça (migração V1, resolver de tenant, seed do admin) tinha uma premissa quebrada quando o banco nasce vazio.

> ⚠️ Erro lateral diagnosticado no caminho: um `.class` obsoleto (build da IDE sem auto-compile) fez o fix nº 3 parecer não funcionar. Não é decisão de arquitetura — registrado abaixo só como nota operacional.

---

## Decisão

Quatro correções pontuais, mantendo a arquitetura da ADR-024 intacta e garantindo que **produção receba as configurações corretas** (Flyway ativo, migrações reais), com os workarounds restritos ao ambiente local.

### 1. V1 reescrita — de `ALTER TABLE` para `CREATE SCHEMA`

A V1 original assumia que as tabelas já existiam em `crm_db` e apenas adicionava a coluna `clinic_id`:

```sql
-- ANTES (quebrava em banco limpo: tabela não existe ainda)
ALTER TABLE crm_db.customers ADD COLUMN IF NOT EXISTS clinic_id UUID;
UPDATE crm_db.customers SET clinic_id = '...';
ALTER TABLE crm_db.customers ALTER COLUMN clinic_id SET NOT NULL;
-- ... idem contact_logs, lead_tickets
```

Como as entidades **já carregam** `@TenantId UUID clinicId` (ADR-024) e `@Table(schema = "crm_db" | "identity_db")`, o Hibernate cria as tabelas já com a coluna correta. A V1 só precisa **garantir os schemas**:

```sql
-- DEPOIS
CREATE SCHEMA IF NOT EXISTS crm_db;
CREATE SCHEMA IF NOT EXISTS identity_db;
```

> 📐 A V1 deixa de ser uma migração de dados legados (que fazia sentido sobre um banco com tabelas em produção) e passa a ser o **bootstrap de schemas** de um projeto greenfield — coerente com a decisão de zerar o banco de produção.

### 2. `ddl-auto`: `validate` → `update` (fase greenfield)

Com a V1 criando apenas os schemas, **não há migração que crie as tabelas**. Quem cria é o Hibernate. Portanto `validate` (que falha se a tabela não existe) é incompatível com a estratégia atual. `update` deixa o Hibernate ser o dono do DDL de tabelas enquanto o schema ainda muda a cada feature.

| Critério | `validate` + Flyway DDL completo | `update` (Hibernate dono) | Peso |
|---|---|---|---|
| Funciona em banco limpo sem CREATE TABLE | ❌ | ✅ | Alto |
| Esforço agora | Alto (escrever todo o DDL) | ✅ Zero | Alto |
| Segurança em prod a longo prazo | ✅ Auditável | ⚠️ Hibernate decide sozinho | Alto |
| Adequado à fase (schema instável) | ⚠️ Retrabalho a cada entidade | ✅ | Médio |

🎯 `update` é a escolha correta **para a fase atual** (schema mudando, prod descartável). É débito explícito — ver "Migração futura".

### 3. Flyway desabilitado **apenas no local** (PostgreSQL 18)

O Flyway 11.14.1 não suporta PostgreSQL 18.3 (`Unsupported Database`). O banco local roda PG18; o Railway (prod) roda PG16/17, suportado.

```properties
# application-local.properties (gitignored — fica só na máquina do dev)
spring.flyway.enabled=false
```

```properties
# application-prod.properties — blindagem explícita
spring.flyway.enabled=true
```

> 📐 **Separação por ambiente**: o workaround vive em `application-local.properties` (que está no `.gitignore` por conter segredos). Prod **nunca** herda o disable. Para reforçar, `application-prod.properties` declara `spring.flyway.enabled=true` explicitamente — defesa contra uma config compartilhada futura desligar o Flyway sem querer.

### 4. `ClinicResolveTenant` retorna sentinela em vez de `null`

O Hibernate 7, quando `@TenantId` torna a `SessionFactory` multi-tenant, exige um identificador de tenant **não-nulo em toda abertura de sessão** — inclusive nas que o Spring Data abre no bootstrap para checar named queries (`NamedQuery.hasNamedQuery`). Fora de request (startup, jobs), `TenantContext.get()` é `null` → exceção.

```java
private static final UUID NO_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000000");

@Override
public UUID resolveCurrentTenantIdentifier() {
    UUID current = TenantContext.get();
    return current != null ? current : NO_TENANT;   // nunca null
}
```

**Por que é seguro:**
- Entidades com `@TenantId` passam a filtrar por `NO_TENANT` fora de request → resultado vazio. Nenhuma clínica real tem o UUID zero, então não há vazamento entre tenants.
- Entidades **sem** `@TenantId` (`User`, `PermissionRule`) não são filtradas — login (`findByUsername`) e RBAC funcionam normalmente fora de qualquer tenant.
- A regra do §6 da ADR-024 continua valendo: operações multi-tenant intencionais (listeners, jobs) **devem** setar o `TenantContext` explicitamente. O sentinela só evita o crash no boot e em contextos sem tenant — não é um tenant "default" para escrita.

### 5. Propriedade órfã `app.admin.clinicId`

O `UserSeeder` injeta `@Value("${app.admin.clinicId}")` para criar o admin do primeiro boot, mas a propriedade não existia. Adicionada seguindo o padrão das demais (`ADMIN_*`), com a clínica única do projeto como default:

```properties
app.admin.clinicId=${ADMIN_CLINIC_ID:00000000-0000-0000-0000-000000000001}
```

---

## Garantia para produção

| Config | Local | Prod (Railway) |
|---|---|---|
| `spring.flyway.enabled` | `false` (PG18) | **`true`** (explícito) |
| `ddl-auto` | `update` | `update` (greenfield) |
| V1 (CREATE SCHEMA) | não roda (Flyway off) | **roda** — cria os schemas |
| Tabelas | Hibernate `update` | Hibernate `update` |
| `app.admin.clinicId` | default `...001` | via env `ADMIN_CLINIC_ID` |

No boot de um prod limpo: Flyway executa V1 (`CREATE SCHEMA`), depois o Hibernate cria as tabelas nos schemas corretos com `clinic_id`. O `UserSeeder` cria o admin na clínica injetada por env.

---

## Arquivos atingidos

| Arquivo | Mudança |
|---|---|
| `db/migration/V1__create_schemas.sql` | `ALTER TABLE` → `CREATE SCHEMA IF NOT EXISTS` (crm_db, identity_db). **Renomeado em 2026-06-26** de `V1__add_clinic_id_to_crm_tables.sql` — ver nota abaixo |
| `application.properties` | `ddl-auto` `validate`→`update`; `+ app.admin.clinicId` |
| `application-local.properties` (gitignored) | `+ spring.flyway.enabled=false` |
| `application-prod.properties` | `+ spring.flyway.enabled=true` (blindagem explícita) |
| `config/tenant/ClinicResolveTenant.java` | sentinela `NO_TENANT` em vez de `null` |

> **Atualização 2026-06-26 — renomeado.** O projeto do Railway (e seu banco) foi apagado para corrigir o back ponta a ponta. Sem banco, não há `flyway_schema_history` → renomear a V1 deixou de ter risco de checksum. A V1 passou de `V1__add_clinic_id_to_crm_tables.sql` (nome fóssil, descrevia um backfill que o conteúdo não faz) para **`V1__create_schemas.sql`**, refletindo o que ela realmente é: o bootstrap de schemas do greenfield. Conteúdo inalterado (os dois `CREATE SCHEMA IF NOT EXISTS`).
>
> 🔑 **Regra para mudanças futuras no Flyway:** renomear/alterar uma migration só é seguro **antes** dela rodar num banco que persista o `flyway_schema_history`. Depois que prod subir e gravar o history, alterar V1 (nome, conteúdo) quebra o boot por checksum mismatch — aí o caminho é uma migration nova (V2+), nunca editar a V1.

---

## Consequências positivas

- Boot ponta a ponta em banco limpo funciona — a fundação da ADR-024 está exercitada.
- Prod recebe as configurações corretas: Flyway ativo, schemas via migração, admin por env.
- Workaround de PG18 isolado no local; zero impacto em produção.
- Sentinela de tenant resolve o boot sem enfraquecer o isolamento (UUID zero nunca pertence a clínica real).

## Consequências negativas / riscos e débitos

| Débito / risco | Mitigação / plano |
|---|---|
| `ddl-auto=update` em prod não é auditável a longo prazo | **Migração futura**: quando o schema estabilizar, escrever migrações Flyway com o DDL completo (CREATE TABLE por schema) e voltar `ddl-auto=validate`. Abrir ADR específica |
| ~~Nome do arquivo V1 não reflete o conteúdo~~ | ✅ **Resolvido (2026-06-26)**: renomeado para `V1__create_schemas.sql` na janela sem banco (sem history → sem risco de checksum) |
| Módulos futuros (Deal, agendamento…) podem ser implementados achando que precisam de migration própria | **Não precisam** enquanto `ddl-auto=update` for o dono do DDL de tabelas. A V1 (schemas) é a única migration que deve existir nesta fase. DDL de tabela no Flyway só entra na migração para `validate` (ver "Migração futura" acima) |
| Dois UUIDs mágicos: `...000` (NO_TENANT) e `...001` (clínica padrão) espalhados | Centralizar como constantes (`TenantConstants`) numa próxima refatoração |
| `app.admin.clinicId` fixo pressupõe clínica única | Quando o cadastro multi-clínica entrar, o seed do admin deve criar a clínica e derivar o ID, não usar constante. Reabrir no ADR de onboarding de clínica |
| Flyway preso à versão que não suporta PG18 | Atualizar o Flyway quando houver release com suporte a PG18, e então remover o disable local |

---

## Nota operacional (não-arquitetural)

Durante a depuração, o fix nº 4 (sentinela) pareceu não funcionar porque a IDE rodou um `.class` obsoleto (auto-build desligado + restart do DevTools com bytecode antigo). Lição: o Spring DevTools só recarrega o que foi **recompilado**. Manter o auto-build da IDE ligado, ou `Rebuild Project` antes de restart manual.

---

## Alternativas descartadas

- **Manter `validate` e escrever todo o DDL em Flyway agora**: descartado nesta fase — schema ainda muda a cada feature; seria retrabalho a cada entidade. É o destino futuro, não o passo de agora.
- **Lançar exceção quando `TenantContext` é null** (em vez do sentinela): descartado — quebraria o bootstrap do Spring Data, que abre sessões sem tenant para inspecionar named queries.
- **Desabilitar multi-tenancy no boot**: descartado — exigiria desligar `@TenantId`, contradizendo a ADR-024.
- **Fazer downgrade do PostgreSQL local para 17**: válido, mas mais custoso que desabilitar o Flyway no local; não resolve o suporte a PG18 que virá de qualquer forma.

---

## Referências

- ADR-024 — `@TenantId` + `TenantContext` (esta ADR corrige o boot da fundação; o §6 sobre contextos sem tenant continua válido)
- ADR-022 — clínica única em produção (origem do `...001`)
- ADR-014 — Flyway como ferramenta de migração
