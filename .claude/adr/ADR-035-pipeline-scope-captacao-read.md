# ADR-035: Escopo `PIPELINE` — visibilidade read-only da captação até avaliação

**Status**: Implementado — 2026-07-01
**Data**: 2026-07-01
**Autores**: Arquiteto-Agent
**Impacto**: `PermissionScope`, `PermissionService.resolveScope`, `LeadTicketSpecifications`, `CustomerSpecifications`, `ContactLogSpecifications`, `ProcedureSpecifications`, `InstallmentSpecifications`, `AppointmentSpecifications`, `PermissionSeeder`, CLAUDE.md
**Relaciona**: ADR-011 (escopo `INTAKE` cross-sector — esta ADR o estende para leitura)

---

## Contexto

O escopo `INTAKE` (ADR-011) dá à captação (`USER_LEADS`, `ADM_LEADS`, `USER_ATTENDANT`) acesso cross-sector aos recursos cujo ticket está em `{LEADS, ATTENDANT}`. Problema observado em produção (2026-07-01): quando um ticket avança (`→ SCHEDULED` move `currentSector` para `EVALUATOR`), o Customer/ticket **sai da visão da captação**. Consequência concreta:

- A captação não enxerga mais o paciente que ela captou assim que a avaliação começa.
- Ao tentar recadastrar (por acreditar que o CPF está livre), bate no `uq_customer_cpf` → erro. (O dedup em si foi resolvido separadamente tornando a checagem de CPF tenant-global no `create`.)

O cliente pediu para **ampliar a visibilidade da captação até a fase de avaliação** — mas **não** dentro do comercial, e **sem** dar à captação poder de editar/transicionar tickets que já estão com o avaliador.

## Decisão

Criar um novo `PermissionScope.PIPELINE` = recursos cujo ticket está em `{LEADS, ATTENDANT, EVALUATOR}` (exclui `COMMERCIAL`, `ADM`, `MANAGER`), e usá-lo **apenas nas regras de READ** da captação. As regras de **UPDATE** permanecem em `INTAKE` (`{LEADS, ATTENDANT}`).

Separar READ de UPDATE exige um segundo conjunto de setores — daí um escopo novo em vez de alargar o `INTAKE` (que arrastaria o UPDATE junto).

## Análise de Trade-offs

| Critério | Alargar `INTAKE` (read+update) | Novo escopo `PIPELINE` (só read) | Peso |
|---|---|---|---|
| Separação de responsabilidade (avaliador dono da avaliação) | ❌ captação passa a transicionar tickets em avaliação | ✅ update continua no boundary atual | Alto |
| Complexidade | ✅ zero enum novo | ⚠️ +1 valor de escopo (tratado em 6 switches) | Médio |
| Aderência ao pedido do cliente ("só visibilidade") | ❌ dá edição junto | ✅ exatamente visibilidade | Alto |
| Risco de regressão | ⚠️ amplia superfície de escrita | ✅ leitura não muda invariantes | Alto |

🎯 **`PIPELINE` (só leitura) preserva a fronteira de escrita do avaliador — o pedido era visibilidade, não poder de ação. O custo é um valor de enum a mais, tratado uma vez em cada `byScope`.**

## Implementação

- `PermissionScope.PIPELINE` adicionado ao enum.
- `PermissionService.resolveScope`: `PIPELINE` → `isIntakeSector(user) && isPipelineSector(target)`, onde `isPipelineSector ∈ {LEADS, ATTENDANT, EVALUATOR}` (caminho single-resource, ex.: `findById`).
- `byScope` de `LeadTicket`/`Customer`/`ContactLog`: `PIPELINE` filtra ticket em `{LEADS, ATTENDANT, EVALUATOR}` (list/search).
- `byScope` de `Procedure`/`Installment`/`Appointment`: `PIPELINE` cai no ramo `UnsupportedOperationException` (não usam esse escopo) — necessário só para o switch exaustivo compilar.
- `PermissionSeeder`: READ de `CUSTOMER`/`TICKET`/`CONTACT_LOG` da captação migrado de `INTAKE`/`SECTOR` → `PIPELINE`. Todos os UPDATE permanecem em `INTAKE`.

## Consequências

**Positivas**
- Captação acompanha o lead do intake até a avaliação sem perder o histórico ao avançar.
- Fronteira de escrita intacta — captação não transiciona tickets do avaliador.
- Padrão reaproveitável: qualquer read que precise "ver o funil até avaliação" usa `PIPELINE`.

**Negativas / riscos**
- +1 valor em `PermissionScope` que **todo** `switch` sobre o enum precisa tratar (são exaustivos, sem `default`). Documentado aqui para o próximo dev não ser pego de surpresa.
- Captação passa a ver contact logs de tickets em avaliação (coerente: vê o ticket, vê a timeline).

## Alternativas descartadas

- **Alargar `INTAKE` para incluir `EVALUATOR`**: descartada — arrastaria as regras de UPDATE junto, dando à captação poder de transicionar tickets em avaliação (contra o pedido "só visibilidade").
- **Abrir o `search()` global para todos**: descartada — misturaria os pipelines de todos os setores na worklist de cada papel, desfazendo o propósito do escopo.

## Referências

- ADR-011 — escopo `INTAKE` cross-sector (base estendida por esta ADR)