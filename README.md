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

First create docker compose file, like in the example below

```yaml
version: "3.8"

services:
  api:
    container_name: shared_finances-api
    restart: unless-stopped
    build:
      context: ./backend
      dockerfile: ./Dockerfile
    networks:
      - shared-finances
    environment:
      POSTGRES_CONNECTION_STRING: host.docker.internal:5432
      POSTGRES_DB: shared_finances
      POSTGRES_USER: shared_finances
      POSTGRES_PASSWORD: shared_finances
      GOOGLE_APPLICATION_CREDENTIALS: /home/config/firebase/shared_finances_service-account.json
      PORT: 8080
      JOB_TIMEZONE: America/Sao_Paulo
    extra_hosts:
      - "host.docker.internal:host-gateway"
    volumes:
      - /home/config/firebase/:/home/config/firebase/
  nginx:
    container_name: shared_finances-nginx
    restart: unless-stopped
    build:
      context: ./frontend
      dockerfile: ./Dockerfile
      args:
        - NG_APP_SERVER_URL=finances.gabrielsilva.dev
        - NG_APP_USE_SSL=true
    networks:
      - shared-finances
    depends_on:
      - api
    ports:
      - 8080:80
    command: "/bin/sh -c 'while :; do sleep 6h & wait $${!}; nginx -s reload; done & nginx -g \"daemon off;\"'"

networks:
  shared-finances:
```

And finally

`docker compose up -d`

### Standalone
- [Running backend](/backend/README.md).
- [Running frontend](/backend/README.md).


