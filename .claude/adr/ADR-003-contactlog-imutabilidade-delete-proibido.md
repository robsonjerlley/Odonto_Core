# ADR-003: ContactLog é imutável — endpoint DELETE removido

**Status**: Aceito  
**Data**: 2026-05-28  
**Autores**: Arquiteto-Agent  
**Impacto**: Módulo funnel — ContactLogController, ContactLogService, ContactLogServiceImpl

---

## Contexto

O `ContactLogController` expõe `DELETE /api/v1/contact-logs/{id}` e o service implementa o método `delete(UUID id)`. Ao revisar o `PermissionSeeder`, constata-se que nenhum perfil possui `Action.DELETE` para `Resource.CONTACT_LOG`:

| Perfil | CREATE | READ | DELETE |
|---|:---:|:---:|:---:|
| ADM_SYSTEM | ✅ GLOBAL | ✅ GLOBAL | ❌ |
| ADM_LEADS | ✅ SECTOR | ✅ SECTOR | ❌ |
| USER_LEADS | ✅ OWN | ✅ OWN | ❌ |
| USER_ATTENDANT | ✅ OWN | ✅ OWN | ❌ |
| ADM_EVALUATOR | — | ✅ SECTOR | ❌ |
| USER_EVALUATOR | — | ✅ GLOBAL | ❌ |
| ADM_COMMERCIAL | — | ✅ SECTOR | ❌ |
| USER_COMMERCIAL | — | ✅ GLOBAL | ❌ |

A ausência de regra DELETE não é uma omissão — é intencional. ContactLog é o registro de auditoria do funil: documenta cada interação, mudança de status e nota registrada sobre um ticket. Deletar um ContactLog significa apagar história do relacionamento com um lead.

O endpoint existe mas não deveria. Qualquer usuário autenticado conseguia deletar registros de auditoria porque o service não tinha RBAC — e mesmo que tivesse, não há regra no seeder para bloquear.

---

## Decisão

`DELETE /api/v1/contact-logs/{id}` é removido permanentemente da API.

- `ContactLogController`: remover o método `delete()` e a anotação `@DeleteMapping("/{id}")`
- `ContactLogService`: remover a declaração `void delete(UUID id)` da interface
- `ContactLogServiceImpl`: remover a implementação do método `delete(UUID id)`
- `PermissionSeeder`: nenhuma regra DELETE para CONTACT_LOG será adicionada agora nem no futuro sem nova ADR explícita

---

## Consequências Positivas

- API e seeder ficam consistentes — o contrato exposto reflete exatamente o que o modelo de permissões permite
- Integridade do histórico de auditoria garantida por design, não só por RBAC
- Surface de ataque reduzida — um endpoint a menos para proteger
- Qualquer tentativa futura de "só vou remover um log de teste" é bloqueada por contrato, não por convenção

## Consequências Negativas / Riscos

- Nenhum. O endpoint nunca deveria ter existido. Não há consumidor externo e não há regra de permissão que o sustente.

---

## Alternativas Consideradas

- **Adicionar Action.DELETE ao seeder para ADM_SYSTEM**: descartado. Dar ao administrador poder de apagar registros de auditoria cria risco de cobertura de fraude e compromete rastreabilidade. O requisito "quero apagar um log de teste" deve ser resolvido em ambiente de desenvolvimento, não via API de produção.
- **Manter o endpoint com RBAC restritíssimo**: descartado. Não há caso de uso legítimo identificado. Manter código sem uso cria débito de manutenção sem retorno.

---

## Referências Cruzadas

- `security-gaps-funnel-permission.md` — gap originalmente identificado neste arquivo
- `PermissionSeeder.java` — fonte de verdade da matriz RBAC; ausência de DELETE em CONTACT_LOG é deliberada
- `ADR-004` — padrão de chamada ao `checkOrThrow` nos services do módulo funnel