# Render Deployment

## Backend Web Service

Use these settings when creating the Render Web Service for the Spring Boot backend.

```text
Name: briefl-backend
Language: Docker
Branch: main
Region: Ohio (US East)
Root Directory: backend
Dockerfile Path: ./Dockerfile
Instance Type: Free
```

Render auto-deploys from the connected GitHub branch, so no GitHub Actions secret or repository variable is required for the basic CI/CD flow.

## Backend Environment Variables

Set these in the Render Web Service environment variable section.

```text
DATABASE_URL=jdbc:postgresql://<render-postgres-host>:5432/briefl
DATABASE_USERNAME=briefl_user
DATABASE_PASSWORD=<render-postgres-password>

NAVER_CLIENT_ID=<naver-client-id>
NAVER_CLIENT_SECRET=<naver-client-secret>

AI_ANALYSIS_MODE=mock
OPENAI_MODEL=gpt-5.4-nano
OPENAI_MAX_OUTPUT_TOKENS=2000
OPENAI_API_REQUEST_TIMEOUT_SECONDS=30
PORT=8080
```

`OPENAI_API_KEY` is not required while `AI_ANALYSIS_MODE=mock`.
Add it later when switching to `AI_ANALYSIS_MODE=openai`.

## Render Postgres

Use the Render Postgres connection information to fill the database variables.

If Render shows a database URL like:

```text
postgresql://briefl_user:<password>@<host>:5432/briefl
```

split it into Spring datasource variables:

```text
DATABASE_URL=jdbc:postgresql://<host>:5432/briefl
DATABASE_USERNAME=briefl_user
DATABASE_PASSWORD=<password>
```

## Notes

- Render Free web services can sleep after inactivity, so the first request may be slow.
- Render Free Postgres has lifecycle limitations and should not be treated as durable production storage without checking the current Render policy.
- OpenAI and Naver API usage costs are separate from Render infrastructure costs.
