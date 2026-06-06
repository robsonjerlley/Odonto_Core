# ADR-014: Adotar ferramenta de migração de schema (Flyway)

**Status**: Proposto
**Data**: 2026-06-06
**Autores**: Arquiteto-Agent
**Impacto**: `pom.xml`, `application*.properties`, `src/main/resources/db/migration/`, processo de deploy

---

## Contexto

O projeto roda hoje com `spring.jpa.hibernate.ddl-auto=update` e **sem** ferramenta de migração.
O Hibernate ajusta o schema no boot a partir das entidades. Em desenvolvimento isso é cômodo; em
**produção** é débito técnico de fundação:

- `update` **nunca remove nem altera destrutivamente** colunas/constraints — só adiciona. Renomear
  campo, mudar tipo, adicionar `NOT NULL` a coluna existente ou apertar constraint **não** é aplicado,
  gerando *drift* silencioso entre código e banco.
- **Sem revisão**: a alteração de schema não é um artefato versionado revisável em PR.
- **Sem rollback**: não há histórico de versões de schema para reverter.
- **Mudanças de dado** (ex.: reset de `permission_rules` quando o `PermissionSeeder` muda) ficam fora
  de qualquer controle — hoje são SQL manual no console do Railway (ver ADR-013).

À medida que o domínio evolui (campos como `assignedEvaluatorId`, `priceType`, `ContactLog.logType`
já previstos no backlog), o risco de divergência produção × código cresce.

---

## Decisão (proposta)

Adotar **Flyway** (migrations SQL versionadas em `src/main/resources/db/migration/`), com
`ddl-auto=validate` em produção:

- `ddl-auto=validate` → o Hibernate apenas **valida** que o schema bate com as entidades no boot;
  falha rápido se divergir, em vez de mutar o banco.
- Flyway aplica `V1__baseline.sql`, `V2__...` em ordem, com checksum e histórico (`flyway_schema_history`).
- Mudanças de dados (seed de RBAC, resets) viram migrations repetíveis (`R__seed_permissions.sql`) ou
  versionadas, eliminando SQL manual no Railway.

`ddl-auto=update` pode permanecer **apenas** em `application-local.properties` para velocidade de dev.

---

## Trade-offs

| Critério | `ddl-auto=update` (atual) | Flyway + `validate` |
|----------|---------------------------|---------------------|
| Velocidade em dev | ✅ zero esforço | ⚠️ escrever migration |
| Segurança em prod | 🚫 drift silencioso, sem rollback | ✅ versionado, revisável, auditável |
| Alterações destrutivas | 🚫 não aplica | ✅ explícitas e controladas |
| Migração de dados | 🚫 SQL manual | ✅ versionada |
| Curva p/ time solo | ✅ nenhuma | ⚠️ baixa, muito documentada |

🎯 **Recomendo Flyway com `validate` em produção**, porque o custo (escrever migrations) é baixo e
pago uma vez, enquanto o risco de drift produção × código é exatamente o que quebra um MVP quando ele
começa a ter dados reais que não podem ser recriados.

---

## Consequências

**Positivas:** schema versionado, deploy reproduzível, fim do SQL manual no Railway, fail-fast em divergência.
**Negativas / riscos:** exige criar o `V1__baseline.sql` a partir do schema atual (gerar via
`hibernate ddl` ou dump do banco existente) antes de virar a chave — passo único e delicado, precisa
ser feito com o banco de produção espelhado.

## Alternativas Consideradas

- **Liquibase** — equivalente; XML/YAML changelog mais verboso. Flyway (SQL puro) tem curva menor para time Java.
- **Manter `ddl-auto=update`** — descartado para produção pelos riscos acima.

---

## Referências Cruzadas

- `ADR-013` — registra a pré-condição operacional (reset de `permission_rules`) que esta ADR elimina
- `bugs-producao-railway-2026-06-05.md` — Bloco 1E (limpeza manual de `permission_rules`)