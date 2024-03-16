# Shared Finances

> Control your family or individual finances.

This is an open source project, under [MIT](/LICENSE), to help anyone with finances control.

## Demo

You can freely use this project on https://finances.gabrielsilva.dev/.

## Technologies

### Backend
- Kotlin + SpringBoot 3
- Firebase auth

### Frontend
- Angular 15 + Angular Material
- NgRx
- RxStomp

## Running

### Using docker

To run the application using Docker, create a `docker-compose.yml` file as shown in the example below. This setup allows you to use a PostgreSQL database hosted in another container or optionally connect to a local PostgreSQL database for testing purposes.

```yaml
version: "3.8"

services:
  postgres:
    image: postgres:15
    container_name: shared_finances-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: shared_finances
      POSTGRES_USER: shared_finances
      POSTGRES_PASSWORD: shared_finances
    ports:
      - "5432:5432" # Expose the PostgreSQL port for local development (optional)
    volumes:
      - postgres_data:/var/lib/postgresql/data # Persist the database between container restarts
    networks:
      - shared-finances

  api:
    container_name: shared_finances-api
    restart: unless-stopped
    build:
      context: ./backend
      dockerfile: Dockerfile
    networks:
      - shared-finances
    environment:
      POSTGRES_CONNECTION_STRING: "postgres:5432" # Use "host.docker.internal:5432" for local testing
      POSTGRES_DB: shared_finances
      POSTGRES_USER: shared_finances
      POSTGRES_PASSWORD: shared_finances
      GOOGLE_APPLICATION_CREDENTIALS: /home/config/firebase/shared_finances_service-account.json
      PORT: 8080
      JOB_TIMEZONE: America/Sao_Paulo
    volumes:
      - /home/config/firebase:/home/config/firebase
    depends_on:
      - postgres # Remove this line if connecting to a local PostgreSQL server

  nginx:
    container_name: shared_finances-nginx
    restart: unless-stopped
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        - NG_APP_SERVER_URL=finances.gabrielsilva.dev
        - NG_APP_USE_SSL=true
    networks:
      - shared-finances
    depends_on:
      - api
    ports:
      - "8080:80"
    command: "/bin/sh -c 'while :; do sleep 6h & wait $${!}; nginx -s reload; done & nginx -g \"daemon off;\"'"

networks:
  shared-finances:

volumes:
  postgres_data:
```

To run the containers in detached mode, execute:

`docker compose up -d`

Note: If you opt to use a local PostgreSQL instance, ensure it is already running and accessible on the default port (5432) before starting the application containers.

### Standalone
- [Running backend](/backend/README.md).
- [Running frontend](/backend/README.md).


