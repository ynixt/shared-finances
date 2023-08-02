# Shared Finances Backend

## Before Running

### Requisites

- Postgresql 15+
- Java 20
- Firebase account

### Environment variables

Before run this project is necessary edit the file `.dev.env` and `.docker.env` to put the postgresql connection info.
This file contains the environment variables. During developing it's possible to load this file with a help of IDE
plugins, like [EnvFile for IntelliJ](https://plugins.jetbrains.com/plugin/7861-envfile).

### Firebase service account

To run this project is also necessary have the `service-account.json` in `src/main/resources`. This file can be
generated with help of
this [guide](https://sharma-vikashkr.medium.com/firebase-how-to-setup-a-firebase-service-account-836a70bb6646).

## Local running

Just call the gradle command `bootRun` (or just run the main method at `SharedFinancesApplication.kt`) and the
service will be available on `http://localhost:8080`. You can change this port in `.dev.env` file.

## Helpful docs

- [Spring Docs](https://docs.spring.io/spring-framework/reference/)
- [Mapstruct docs](https://mapstruct.org/documentation/stable/reference/html/)
