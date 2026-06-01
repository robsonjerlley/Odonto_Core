# ADR-005: Estratégia de Refresh Token — Single Token com Validação de Assinatura

**Status**: Aceito  
**Data**: 2026-05-31  
**Autores**: Arquiteto-Agent  
**Impacto**: Módulo security — `AuthService`, `AuthController`, `JwtUtil`

---

## Contexto

O sistema utiliza JWT stateless com expiração de 24 horas (`jwt.expiration=86400000`). Após expiração, o cliente recebe 401 e não tem como renovar o token sem fazer login novamente com usuário e senha.

`refreshToken()` está documentado no diagrama de arquitetura como pendente. A decisão necessária é: qual estratégia de renovação adotar?

Existem duas abordagens viáveis:

**Opção A — Single Token**: aceitar o token expirado, validar a assinatura, extrair o username e emitir um novo token. Nenhuma entidade ou tabela nova.

**Opção B — Dual Token**: emitir um access token de curta duração (ex: 15 min) e um refresh token de longa duração (ex: 7 dias) armazenado no banco, com suporte a revogação individual.

---

## Análise de Trade-offs

| Critério | Opção A — Single Token | Opção B — Dual Token |
|---|---|---|
| Complexidade de implementação | Baixa — sem entidade nova | Alta — `RefreshToken` entity + tabela + rotação |
| Segurança | Média — token comprometido pode ser renovado | Alta — revogação individual possível |
| Revogação | Impossível sem blocklist | DELETE no banco |
| Adequação ao contexto (CRM interno) | Suficiente | Overkill para fase atual |
| Time-to-market | Rápido | Mais lento |
| Reversibilidade | Alta — não compromete evolução para B | — |

---

## Decisão

**Opção A — Single Token**, com validação obrigatória de assinatura.

O fluxo de `refreshToken()`:

1. Cliente envia o token expirado via `POST /api/v1/authentication/refresh`
2. `JwtUtil` tenta o parse normal; captura `ExpiredJwtException` e extrai os claims de dentro dela
3. Valida que a assinatura é legítima — tokens adulterados retornam 401 imediatamente
4. Extrai o username dos claims
5. Carrega o usuário no banco — confirma que ainda existe e está ativo
6. Emite novo token com expiração renovada e retorna `AuthResponseDTO`

A extração de claims de um token expirado é feita via `ExpiredJwtException.getClaims()`, disponível no JJWT. Isso requer um novo método em `JwtUtil` — ex: `extractUsernameIgnoringExpiration(String token)` — que captura a exceção e retorna o subject em vez de propagar o erro.

---

## O que precisa ser criado

| Artefato | Tipo | Descrição |
|---|---|---|
| `RefreshTokenRequestDTO` | Record | Campo único: `token` (String, @NotBlank) |
| `JwtUtil.extractUsernameIgnoringExpiration()` | Método | Parse com captura de `ExpiredJwtException` |
| `AuthService.refreshToken(String token)` | Método | Orquestra validação e emissão do novo token |
| `POST /api/v1/authentication/refresh` | Endpoint | `permitAll` — cliente não tem token válido para autenticar |

---

## Consequências Positivas

- Implementação simples, sem schema novo, sem migração
- Experiência do usuário preservada — sem relogin após expiração
- Fundação não compromete migração futura para Dual Token (Opção B)

## Consequências Negativas / Riscos

- Sem revogação: um token comprometido pode ser renovado enquanto o cliente não perceber
- Mitigação aceitável para CRM interno com usuários controlados — risco baixo

## Alternativas Consideradas

- **Dual Token (Opção B)**: descartada para esta fase. Time pequeno, domínio em consolidação, risco de segurança baixo para CRM interno. Pode ser adotada futuramente se o sistema passar a expor dados sensíveis ou atender múltiplos clientes externos.
- **Auto-renovação no filtro JWT**: descartada. Acoplaria lógica de emissão de token ao filtro de autenticação, violando separação de responsabilidades. O filtro valida; o service emite.

---

## Referências Cruzadas

- `JwtUtil.java` — métodos `generateToken`, `extractUsername`, `isExpired`, `isValid`
- `AuthService.java` — `login()` como referência de estrutura
- `AuthController.java` — endpoint de login como referência de padrão REST
- `SecurityConfig.java` — `/api/v1/authentication/**` já está em `permitAll`
