# Project Guidelines — Shared Finances Backend

This document guides Junie (and contributors) when working on this project. It includes a short project overview, structure, and how to build, test, and keep code style consistent.

## Project Overview
- Language/Stack: Kotlin 2.2, Spring Boot 3.5 (WebFlux), Reactive stack (Project Reactor).
- Persistence: PostgreSQL via R2DBC (reactive driver). Database migrations managed by Flyway (`src/main/resources/db/migration`).
- APIs: REST controllers and GraphQL (`src/main/resources/graphql`).
- Security: Spring Security + OAuth2 Authorization Server.
- Caching/Messaging: Reactive Redis (starter included).
- Tooling: Gradle Kotlin DSL, Java Toolchain 24 (managed by Gradle), ktlint for code style.
- Extra: Typescript interface generator task configured to export backend DTOs/enums to the frontend.

## Project Structure (high level)
- `src/main/kotlin/com/ynixt/sharedfinances`
  - `application` — Web layer, configs, controllers (REST/GraphQL), DTOs, mappers, jobs.
  - `domain` — Entities, enums, models, services (interfaces and impl), repositories (ports), exceptions, utils.
  - `resources` — Adapters and infrastructure implementations (Spring Data, R2DBC mappings, external services).
- `src/main/resources`
  - `db/migration` — Flyway SQL migrations.
  - `graphql` — GraphQL schemas.
- `src/test/kotlin` — Unit/integration tests.
- Build config: `build.gradle.kts`, `settings.gradle.kts`.

## How Junie should run tests
- Default: run the test suite for verification when code changes affect logic.
  - CLI: `./gradlew test`
  - In this workspace, prefer the test tool with absolute/relative paths, for example:
    - Run all tests in the project: path = `src/test`
    - Run a specific package: path = `src/test/kotlin/com/ynixt/sharedfinances`
    - Run one test file by full path.
- JUnit Platform is enabled; Kotlin test and Spring Boot test starters are configured.

## Build instructions
- Full build (compiles + tests): `./gradlew build`
- Compile only: `./gradlew assemble`
- Generate Typescript models (if needed by a task): `./gradlew generateTypescriptInterfaces`
- Run the app locally (requires PostgreSQL and Redis configured in Spring properties/env): `./gradlew bootRun`

Note: The Java 24 toolchain is configured in Gradle; you do not need Java 24 installed locally if using Gradle’s toolchain support.

## Code style and quality
- Ktlint is configured via Gradle plugin.
  - Check style: `./gradlew ktlintCheck`
  - Auto-fix: `./gradlew ktlintFormat`
- Keep Kotlin idioms and the existing package structure. Mirror the patterns used in nearby code for consistency.

## What Junie should do before submitting a change
- If you changed any Kotlin code:
  - Run relevant tests, or the whole test suite if unsure.
  - Ensure `ktlintCheck` passes (or run `ktlintFormat` first).
- For documentation-only changes, tests/build are not required unless explicitly requested by the task.
- If you changed any Kotlin code inside `src/main/kotlin/com/ynixt/sharedfinances/application/web/dto`:
  - Run `generateTypescriptInterfaces`

## Notes
- Database migrations live under `src/main/resources/db/migration` and should be added when schema changes are introduced.
- GraphQL schema updates go under `src/main/resources/graphql`; remember to keep DTOs and mappers aligned.
- Our frontend is located on `../frontend/.junie/guidelines.md`. To know more about check the `guidelines.md` inside frontend folder.
- `generateTypescriptInterfaces` generates models inside frontend, on folder `frontend/src/app/models/generated/**`.
