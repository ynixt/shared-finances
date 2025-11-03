# Shared Finances — Development Notes (Backend + Frontend)

This document captures project-specific practices that help advanced contributors build, test, and evolve the codebase efficiently. It supplements, not replaces, the repository README.


## Quickstart (Backend + Frontend)
Backend
- Build + test: `cd backend && ./gradlew build`
- Run app (DB/Redis must be up): `cd backend && ./gradlew bootRun`
- Style check/fix: `cd backend && ./gradlew ktlintCheck` | `ktlintFormat`
- Generate TS models for frontend: `cd backend && ./gradlew generateTypescriptInterfaces`
- Note: the Gradle wrapper lives in the `backend/` folder. Always run Gradle commands from inside `backend/` (or use `backend/gradlew ...`).

Frontend
- Install deps (Node 20.x):
```bash
cd frontend
npm ci
```
- Dev server: `npm start`
- Build: `npm run build`
- Lint check/fix: `npm run lint` | `npm run lint:fix`
- Only Prettier: `npm run prettier` | `npm run prettier:fix`
- Only ESLint: `npm run eslint` | `npm run eslint:fix`


## Tech Stack Snapshot
- Backend: Kotlin 2.2, Spring Boot 3.5 (WebFlux, Reactive stack), R2DBC (PostgreSQL), Flyway migrations, Reactive Redis starter included.
- Security: Spring Security + OAuth2 Authorization Server.
- Backend Build: Gradle Kotlin DSL, Java Toolchain 24 (auto-provisioned by Gradle).
- Backend Lint/Style: ktlint Gradle plugin.
- GraphQL: Schemas under `backend/src/main/resources/graphql`.
- Backend Types export to frontend: DTOs/enums → Typescript via `another-typescript-generator` task.
- Frontend: Angular 20 workspace under `frontend/`.
- Frontend build: NPM, node >= 20
- Frontend Lint/Style: ESLint, Prettier.
- Frontend i18n: YAML files under `frontend/src/i18n/**`.


## Repository Layout
- `backend/` — Spring Boot application
  - Kotlin sources: `backend/src/main/kotlin/com/ynixt/sharedfinances/**`
  - Tests: `backend/src/test/kotlin/**`
  - Flyway migrations: `backend/src/main/resources/db/migration/**`
  - Build config: `backend/build.gradle.kts`, `backend/settings.gradle.kts`
- `frontend/` — Angular application
- `docker-compose-local.yml` — Local Postgres/Redis services
- `bruno/` — Bruno API collections for manual/exploratory API calls


## Build & Run — Backend
- Compile only: `./gradlew -p backend assemble`
- Full build + tests: `./gradlew -p backend build`
- Run app (expects DB/Redis reachable, see next section): `./gradlew -p backend bootRun`

Java 24 toolchain is declared in `backend/build.gradle.kts`:
```kotlin
java {
    toolchain { languageVersion = JavaLanguageVersion.of(24) }
}
```
Gradle will provision a matching JDK if not present locally.


## Build & Run — Frontend
- Node version: use Node 20.x (LTS). Install deps once:
```bash
cd frontend
npm ci
```
- Start dev server: `npm start`
- Production build: `npm run build`
- Lint & format:
  - Check: `npm run lint` (runs `eslint .` and `prettier --check .`)
  - Fix: `npm run lint:fix` (runs `eslint . --fix` and `prettier . --write`)
- Individual tools:
  - ESLint: `npm run eslint` | `npm run eslint:fix`
  - Prettier: `npm run prettier` | `npm run prettier:fix`


## Local Services (DB/Redis)
Spring context tests or local runs that touch persistence/caching need Postgres and Redis. For quick local infra:

- Bring up services:
```bash
docker compose -f docker-compose-local.yml up -d
```
- Tear down:
```bash
docker compose -f docker-compose-local.yml down -v
```

If you prefer Testcontainers for integration tests, see Testing section for guidance.


## Database Migrations (Flyway)
- Location: `backend/src/main/resources/db/migration`
- Naming: `V<version>__<Description>.sql` (e.g., `V9__CreateDebitEntry.sql`)
- Flyway runs on app startup; ensure migrations remain idempotent across environments.


## Typescript Interface Generation
When changing backend DTOs or enums under:
- `com.ynixt.sharedfinances.application.web.dto`
- `com.ynixt.sharedfinances.domain.enums`

Run the generator to sync the frontend types:
```bash
./gradlew -p backend generateTypescriptInterfaces
```
Configured in `backend/build.gradle.kts`:
```kotlin
tasks.named<GenerateTypescriptInterfacesTask>("generateTypescriptInterfaces") {
    outputPath = "../frontend/src/app/models/generated"
    classPackages = listOf(
        "com.ynixt.sharedfinances.application.web.dto",
        "com.ynixt.sharedfinances.domain.enums",
    )
    excludeClassPackages = listOf(
        "com.ynixt.sharedfinances.application.web.dto.kratos",
    )
    generateEnumObject = true
}
```


## Backend Code Style & Quality
- Check style: `./gradlew -p backend ktlintCheck`
- Auto-fix: `./gradlew -p backend ktlintFormat`

Follow the existing package structure and mapper patterns (see `application/web/mapper/**`). Prefer pure functions and DTO mappers in `impl/` for easier testability.


## Frontend Code Style & Linting (ESLint + Prettier)
The Angular workspace uses ESLint for static analysis and Prettier for formatting. Scripts are wired in `frontend/package.json`:
- Lint (check only): `npm run lint` → runs `eslint .` and `prettier --check .`
- Lint (auto-fix): `npm run lint:fix` → runs `eslint . --fix` and `prettier . --write`
- Prettier only (check): `npm run prettier`
- Prettier only (write): `npm run prettier:fix`
- ESLint only (check): `npm run eslint`
- ESLint only (fix): `npm run eslint:fix`

Notes
- Import sorting: `@trivago/prettier-plugin-sort-imports` is configured; let Prettier reorder imports instead of ESLint rules.
- Run from `frontend/` directory after `npm ci` with Node 20.x.
- If Prettier complains about line endings on Windows, set `git config core.autocrlf input` or add an `.editorconfig` entry to normalize EOLs.
- Tailwind/Angular templates: ESLint covers TS/HTML; Prettier formats TS/HTML/JSON/MD based on its config.


## Testing — Backend
JUnit Platform is enabled. Dependencies include Spring Boot Test, reactor-test, Kotlin test, and security-test.

### Run backend tests
- All backend tests: `./gradlew -p backend test`
- One class (example):
```bash
./gradlew -p backend test --tests "com.ynixt.sharedfinances.SharedFinancesApplicationTests"
```
- One method (example):
```bash
./gradlew -p backend test --tests "com.ynixt.sharedfinances.SharedFinancesApplicationTests.demoPureUnitTest"
```

### What to test by default on backend
- Prefer pure unit tests that do not require Spring context, external DB, or Redis. They run fast and deterministically in CI.
- For persistence or WebFlux slice tests, use slice annotations and avoid full context when possible.

### Integration tests requiring DB/Redis
Avoid tying tests to developer machines. Options:
- Testcontainers: Spin up Postgres/Redis on demand. Mark tests with a dedicated Gradle/JUnit tag so they are opt-in locally and run in CI. Example skeleton:
```kotlin
@Tag("integration")
@Testcontainers
@SpringBootTest
class SomeIntegrationTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
```
Run only integration tests:
```bash
./gradlew -p backend test --tests "*IntegrationTest" --info
```
Or via tags with JUnit Platform filters.

### Demo test that was executed now
To keep the repo’s test suite runnable without external services, the example Spring context smoke test was replaced with a pure unit test at:

- File: `backend/src/test/kotlin/com/ynixt/sharedfinances/SharedFinancesApplicationTests.kt`
- Content (excerpt):
```kotlin
class SharedFinancesApplicationTests {
    @Test
    fun demoPureUnitTest() {
        val sum = listOf(1, 2, 3).sum()
        assertEquals(6, sum)
    }
}
```
Verified locally with:
```bash
./gradlew -p backend test --tests "com.ynixt.sharedfinances.SharedFinancesApplicationTests"
```

If you want a Spring context smoke test in addition, keep it under a different class name and guard it with a profile or tag so it only runs when the required services are available.


## Web/GraphQL APIs
- REST and GraphQL coexist. GraphQL schemas live under `backend/src/main/resources/graphql`.
- OpenAPI UI is enabled via `springdoc-openapi-starter-webflux-ui`; when the app is running, Swagger UI should be reachable under the conventional path (verify the exact path if changed by config).


## Security Notes
- OAuth2 Authorization Server is on the classpath. If you add tests interacting with security, leverage `spring-security-test` utilities (e.g., `@WithMockUser`) and WebFlux test support.

## Backend Notes
- Database migrations live under `backend/src/main/resources/db/migration` and should be added when schema changes are introduced.
- GraphQL schema updates go under `backend/src/main/resources/graphql`; remember to keep DTOs and mappers aligned.
- `generateTypescriptInterfaces` generates models inside frontend, on folder `frontend/src/app/models/generated/**`.

## Frontend Notes
- Public assets live under `frontend/public/` and are emitted under `frontend/public/` in the final bundle (keeps relative paths stable).
- i18n YAMLs are colocated under `frontend/src/i18n/**` and merged by language key based on filename stem (e.g., `en-US.yaml`, `pt-BR.yaml`). Duplicate keys across files are deep-merged with `deepmerge-ts`.
- Generated models under `frontend/src/app/models/generated/**` should not be hand-edited; treat as read-only. Backend generates files here on Gradle task `generateTypescriptInterfaces`.
- For dev server, builds, and linting commands, see section “Build & Run — Frontend”.

Generated backend types land in `frontend/src/app/models/generated/**`.


## Troubleshooting
- Spring context tests fail locally: likely due to missing Postgres/Redis or misconfigured properties. Prefer pure unit tests or enable Docker compose before running such tests.
- GraphQL scalar issues: ensure `graphql-java-extended-scalars` is aligned (currently 24.0).
- R2DBC pool and Redis connection problems: verify service endpoints from `application.yml`/env.
- Java version mismatch: rely on Gradle toolchains; avoid forcing a local JDK via `JAVA_HOME`.


## Conventions & Tips
- Maintain DTO mappers in `application/web/mapper/impl` to separate mapping logic from controllers.
- Keep Flyway migrations strictly forward-only; never edit existing migration files.
- When adding DTOs/enums for the API, remember to regenerate TS interfaces and commit the generated files in the frontend repository as required by your workflow.
- Use Bruno collections under `bruno/` for quick manual API exploration when developing endpoints.


## Verified Commands (as of 2025-11-02 17:49)
- `./gradlew -p backend test --tests "com.ynixt.sharedfinances.SharedFinancesApplicationTests"` — passes with the included demo unit test.
- `./gradlew -p backend ktlintCheck` — style check for backend.
- `./gradlew -p backend generateTypescriptInterfaces` — emits TS models into the frontend folder.

Frontend (run from `frontend/`):
- `npm run lint` — runs ESLint and Prettier checks.
- `npm run lint:fix` — applies ESLint and Prettier fixes.
- `npm run build` — builds the Angular app.

If any of the above begins failing due to future changes (e.g., reintroducing a Spring context test), re-evaluate local infra requirements or add Testcontainers guards.

## What Junie should do before submitting a change
- If you changed any Kotlin code:
    - Run relevant tests, or the whole test suite if unsure.
    - Ensure `ktlintCheck` passes (or run `ktlintFormat` first).
- If you changed any Typescript our Javascript code:
    - Run relevant tests, or the whole test suite if unsure.
    - Ensure `prettier` passes (or run `prettier:fix` first).
- For documentation-only changes, tests/build are not required unless explicitly requested by the task.
- If you changed any Kotlin code inside `src/main/kotlin/com/ynixt/sharedfinances/application/web/dto` or `src/main/kotlin/com/ynixt/sharedfinances/domain/enums`:
    - Run `generateTypescriptInterfaces`