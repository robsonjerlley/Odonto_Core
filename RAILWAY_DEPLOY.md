# Deploy no Railway — OdontoCore CRM

Guia passo a passo para colocar o backend Spring Boot em produção no Railway.

---

## Pré-requisitos

- Conta no [Railway](https://railway.app) (login com GitHub recomendado)
- Repositório do projeto no GitHub (Railway faz deploy direto do repo)
- Railway CLI (opcional, útil pra logs e env vars): `npm i -g @railway/cli`

---

## Arquivos de configuração já criados

Estes arquivos foram adicionados na raiz do projeto e devem ser commitados:

| Arquivo | Função |
|---|---|
| `nixpacks.toml` | Builda com Java 21 + Maven, define start command |
| `railway.json` | Define builder e política de restart |
| `src/main/resources/application-prod.properties` | Overrides de produção (sem SQL no log, pool de conexões, etc.) |
| `src/main/resources/application.properties` | Agora lê `PORT` e `SPRING_PROFILES_ACTIVE` do ambiente |

---

## Passo 1 — Criar o projeto no Railway

1. Login em railway.app → **New Project** → **Deploy from GitHub repo**
2. Selecione o repositório `odontocore.crm`
3. Railway detecta `nixpacks.toml` e inicia o primeiro build (vai falhar porque ainda não há banco nem env vars — normal)

---

## Passo 2 — Provisionar PostgreSQL

1. Dentro do projeto, clique em **+ New** → **Database** → **Add PostgreSQL**
2. Railway cria um serviço Postgres com variáveis prontas:
   - `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`
   - `DATABASE_URL` (formato `postgresql://...`, **não usável direto** pelo Spring Boot)

---

## Passo 3 — Configurar variáveis de ambiente no serviço da app

Vá no serviço da aplicação (não no Postgres) → aba **Variables** → **+ New Variable**.

### Obrigatórias

| Variável | Valor | Observação |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Ativa `application-prod.properties` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` | Use referência ao serviço Postgres |
| `SPRING_DATASOURCE_USERNAME` | `${{Postgres.PGUSER}}` | Referência |
| `SPRING_DATASOURCE_PASSWORD` | `${{Postgres.PGPASSWORD}}` | Referência |
| `JWT_SECRET` | _(gere uma string aleatória de 64+ chars)_ | Use `openssl rand -hex 32` ou similar |
| `ADMIN_PASSWORD` | _(senha forte)_ | Usada pelo `UserSeeder` no primeiro boot |

### Opcionais (têm default)

| Variável | Default | Quando customizar |
|---|---|---|
| `ADMIN_USERNAME` | `admin` | Se quiser outro username inicial |
| `ADMIN_NAME` | `Administrador` | Nome exibido do admin |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | **Trocar** pelo domínio do frontend em prod (ex: `https://app.odontocore.com`). Múltiplos: separe com vírgula |

> **Sintaxe `${{Postgres.PGHOST}}`** é o jeito do Railway referenciar variáveis de outro serviço do mesmo projeto. Funciona apenas na UI; não escreva literalmente no `application.properties`.

---

## Passo 4 — Gerar domínio público

1. Serviço da app → aba **Settings** → **Networking** → **Generate Domain**
2. Railway gera algo como `odontocore-crm-production.up.railway.app`
3. Este será o `baseURL` que o frontend (futuro) vai consumir

---

## Passo 5 — Redeploy

Após configurar todas as env vars, clique em **Deploy** (canto superior direito) ou faça um push novo no repositório. O build agora deve concluir.

### Verificar boot

Aba **Deployments** → clique no deploy mais recente → **View Logs**. Procure por:

```
=== ADMIN PADRÃO CRIADO: username='admin' — altere a senha imediatamente ===
Started Application in X.XXX seconds
```

### Testar autenticação

```bash
curl -X POST https://seu-dominio.up.railway.app/api/v1/authentication/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"SUA_ADMIN_PASSWORD"}'
```

Deve retornar `200 OK` com um JWT no body/header.

---

## Custos e limites

- **Hobby Plan ($5/mês)**: ~500h de execução, 1GB RAM, Postgres incluso — suficiente pra MVP
- **Free trial**: $5 de crédito uma vez, depois precisa do Hobby
- Railway hiberna serviços ociosos do plano free; no Hobby fica sempre on

---

## Troubleshooting

### Build falha com "no main manifest attribute"
- Verifique se o `spring-boot-maven-plugin` está no `pom.xml` (já está). O fat jar fica em `target/odontocore.crm-0.0.1-SNAPSHOT.jar`.

### App sobe mas retorna 502 ao acessar
- Confira que `server.port=${PORT:8080}` está no `application.properties` (já está) — Railway injeta `PORT` em runtime.
- Verifique nos logs se o banco conectou. Se falhou, revise as referências `${{Postgres.PGHOST}}`.

### Versão do jar mudou
- Se você bumpar a versão no `pom.xml` (ex: `0.0.2-SNAPSHOT`), atualize o nome do jar no `nixpacks.toml` (`[start]` cmd).

### CORS bloqueando o frontend
- Atualize `CORS_ALLOWED_ORIGINS` com o domínio exato do frontend (sem barra no final). Múltiplas origens: `https://app.com,https://staging.app.com`.

### Quero ver logs em tempo real
```bash
railway login
railway link  # selecione o projeto
railway logs
```

---

## Próximos passos (pós-MVP)

- **Healthcheck**: adicionar `spring-boot-starter-actuator` e configurar `healthcheckPath: "/actuator/health"` no `railway.json`
- **Migrations**: trocar `ddl-auto=update` por `validate` + Flyway antes do primeiro release com usuários reais
- **Observabilidade**: Railway expõe métricas básicas; pra mais, plugar Sentry ou um APM
- **Secret rotation**: rotacionar `JWT_SECRET` invalida todos os tokens emitidos — planeje uma janela
