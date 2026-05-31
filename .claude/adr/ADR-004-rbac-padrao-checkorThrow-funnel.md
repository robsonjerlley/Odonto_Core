# ADR-004: Padrão de chamada ao checkOrThrow nos services do módulo funnel

**Status**: Aceito  
**Data**: 2026-05-28  
**Autores**: Arquiteto-Agent  
**Impacto**: Módulo funnel — CustomerServiceImpl, LeadTicketServiceImpl, ContactLogServiceImpl

---

## Contexto

O projeto possui três camadas de proteção:

| Camada | Mecanismo | Garantia |
|---|---|---|
| 1 | `JwtAuthFilter` | Bloqueia qualquer request sem token JWT válido |
| 2 | `SecurityUtils.getCurrentUser()` | Resolve o `User` do SecurityContext |
| 3 | `PermissionService.checkOrThrow()` | Aplica RBAC: role + sector + resource + action + scope |

A revisão cruzada entre `PermissionSeeder` e os services do módulo funnel identificou ausência sistemática da camada 3 — RBAC — na maioria das operações. Os gaps críticos são listados em `security-gaps-funnel-permission.md`.

Antes de implementar o `checkOrThrow` nos services, é necessário estabelecer o padrão correto de chamada, porque a assinatura do método tem uma sutileza que impacta o comportamento por scope:

```java
public void checkOrThrow(User user, Resource resource, Action action,
                         Sector targetSector, UUID targetOwnerId)
```

A resolução de scope funciona assim:

```java
case GLOBAL -> true;
case SECTOR -> user.getSector().equals(targetSector);
case OWN    -> user.getId().equals(targetOwnerId);
```

`targetSector` e `targetOwnerId` representam o **recurso sendo acessado**, não o usuário. Passar os valores errados silencia o scope enforcement sem lançar exceção.

---

## Decisão

### Padrão 1 — CREATE (recurso ainda não existe)

```java
User user = securityUtils.getCurrentUser();
permissionService.checkOrThrow(
    user,
    Resource.X,
    Action.CREATE,
    user.getSector(),   // setor onde o recurso será criado
    user.getId()        // usuário é o dono do que vai criar
);
```

`targetOwnerId = user.getId()` é obrigatório para que usuários com scope OWN passem na verificação. Passar `null` quebra silenciosamente perfis OWN.

---

### Padrão 2 — UPDATE / READ / DELETE (recurso já existe)

**Quando usar**: o recurso já existe no banco e precisa ser acessado. A verificação de scope depende de dados do próprio recurso — não do usuário.

```java
User user = securityUtils.getCurrentUser();

TipoRecurso resource = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("..."));

permissionService.checkOrThrow(
    user,
    Resource.X,
    Action.UPDATE,               // ou READ ou DELETE
    resource.getCurrentSector(), // setor do recurso acessado
    resource.getCreatedBy()      // dono do recurso acessado
);
```

O recurso é buscado **antes** do check de permissão. A ordem é intencional: o check precisa dos dados do recurso para resolver scope SECTOR e OWN.

---

### Padrão 3 — SEARCH / listagens paginadas

```java
User user = securityUtils.getCurrentUser();
permissionService.checkOrThrow(
    user,
    Resource.X,
    Action.READ,
    user.getSector(),
    user.getId()
);
// query executa após o check
```

Para listagens, não há um recurso específico para extrair sector/owner. Passa-se os dados do próprio usuário. Isso verifica se o perfil tem READ para o resource — scope enforcement a nível de query (filtrar por setor/owner no SQL) é trabalho futuro definido como Fase 3 no plano de implementação.

---

### Caso especial — changeStatus com restrição de transição por perfil

`checkOrThrow(user, TICKET, Action.UPDATE, ...)` aprova USER_ATTENDANT para UPDATE genérico (o seeder concede). Porém US-FUND-02 proíbe USER_ATTENDANT de transitar para `LOSS` e `IN_CONTACT`.

O `checkOrThrow` não cobre restrições de transição específicas por perfil. Após o check de RBAC, adicionar validação explícita:

```java
permissionService.checkOrThrow(user, Resource.TICKET, Action.UPDATE,
    ticket.getCurrentSector(), ticket.getCreatedBy());

if (user.getRole() == Role.USER_ATTENDANT
        && (status == TicketStatus.LOSS || status == TicketStatus.IN_CONTACT)) {
    throw new AccessDeniedException("Perfil não autorizado para esta transição de status");
}
```

A alternativa de criar `Action.CLOSE_TICKET` ou semelhante foi descartada — ver seção de alternativas.

---

## Plano de Implementação

**Fase 1 — Críticos:**

| Service | Método | Action | Notas |
|---|---|---|---|
| `ContactLogServiceImpl` | `create` | CREATE | Corrigir: passar `user.getId()` em `targetOwnerId` (hoje passa `null`) |
| `LeadTicketServiceImpl` | `create` | CREATE | Injetar `PermissionService`; adicionar check padrão 1 |
| `LeadTicketServiceImpl` | `changeStatus` | UPDATE | Injetar; check padrão 2 + validação explícita de transição |
| `CustomerServiceImpl` | `deleteById` | DELETE | Injetar `PermissionService`; check padrão 2 |
| `LeadTicketServiceImpl` | `deleteById` | DELETE | Injetar; check padrão 2 |

**Fase 2 — Importantes:**

| Service | Método | Action | Notas |
|---|---|---|---|
| `ContactLogServiceImpl` | `findById` | READ | Check padrão 2 |
| `ContactLogServiceImpl` | `search` | READ | Check padrão 3 |
| `CustomerServiceImpl` | `create` | CREATE | Check padrão 1 |
| `CustomerServiceImpl` | `update` | UPDATE | Check padrão 2 |
| `CustomerServiceImpl` | `findById` | READ | Check padrão 2 |
| `CustomerServiceImpl` | `findByCpf` | READ | Check padrão 3 (CPF é busca, não identifica dono) |
| `CustomerServiceImpl` | `search` | READ | Check padrão 3 |
| `LeadTicketServiceImpl` | `findById` | READ | Check padrão 2 |
| `LeadTicketServiceImpl` | `search` | READ | Check padrão 3 |

**Fase 3 — Débito técnico futuro:**

Scope-aware query filtering: `search()` hoje retorna todos os registros para qualquer perfil com READ. O correto é filtrar no SQL por `createdBy = user.getId()` (scope OWN) ou `currentSector = user.getSector()` (scope SECTOR). Requer refactor dos repositories com `Specification` pattern ou queries condicionais.

---

## Consequências Positivas

- Padrão único e documentado — qualquer desenvolvedor sabe como adicionar RBAC em um novo método
- Scope enforcement correto para todos os perfis, incluindo OWN
- Fase 1 resolve os riscos críticos sem depender da Fase 3

## Consequências Negativas / Riscos

- Fase 3 ausente: search retorna dados além do scope do usuário para perfis OWN/SECTOR. Mitigação: Fase 1+2 garantem que o usuário tem ao menos permissão de READ antes de receber os dados.
- Validação explícita de transição em `changeStatus` está acoplada ao role — se a regra de transição mudar, precisa ser atualizada no service além do seeder.

## Alternativas Consideradas

- **Criar `Action.CLOSE_TICKET` para restringir transições LOSS/IN_CONTACT por perfil**: descartado. Aumenta a complexidade do seeder sem benefício proporcional — a regra é específica de um perfil numa transição, não um padrão recorrente.
- **Verificação de scope apenas no seeder (sem check nos services)**: descartado. O seeder define o que é permitido; o service é quem enforma a regra em runtime.
- **Usar `@PreAuthorize` do Spring Security**: descartado. O modelo de permissão do projeto é dinâmico (regras em banco via `PermissionRule`) e com scope (GLOBAL/SECTOR/OWN) que depende de dados da entidade — `@PreAuthorize` com SpEL não consegue resolver `entity.getCreatedBy()` antes do fetch.

---

## Referências Cruzadas

- `security-gaps-funnel-permission.md` — gaps identificados que motivaram este ADR
- `ADR-003` — remoção do endpoint DELETE de ContactLog
- `PermissionService.java` — implementação de `checkOrThrow` e `resolveScope`
- `PermissionSeeder.java` — matriz RBAC completa
- `us-fundacional.md` — US-FUND-02 define restrições de transição para USER_ATTENDANT
