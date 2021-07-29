# English

Cooming soon

# Português

**Demo:** https://financas.gabrielsilva.dev/

Controle suas finanças familiares ou individuais.

## Tecnologias

- Angular
- NestJS
- GraphQL
- Ngnix
- Docker

## Como instalar

### Docker (prod)

1. Insira uma chave de admin firebase em `backend/unk-shared-finances-firebase-adminsdk.json`;
2. Altere o arquivo `init-letsencrypt.sh` substituindo o domínio `financas.gabrielsilva.dev` pelo seu domínio e o email `admin@unkapps.com` pelo seu email;
3. Altere o arquivo `frontend/environments/environment.prod.ts` para que graphqlWebsocketUrl aponte para a url de websocket correta;
4. Execute o comando `chmod +x .init-letsencrypt.sh/`;
5. Execute o comando `chmod +x mongodb/initiate_replica.sh`;
6. Execute o comando  `docker-compose build`;
7. Execute o comando `sudo ./init-letsencrypt.sh`.

### Local

#### Requisitos
- Mongodb em modo replica
- Node

#### Executando

1. Crie uma variável de ambiente com nome `GOOGLE_APPLICATION_CREDENTIALS` e como valor o caminho para o arquivo de chave de admin do firebase;
2. Execute o comando `npm i` na pasta `backend` e também na pasta `frontend`;
3. Execute o comando `npm start` na pasta `backend` para iniciar o backend e o mesmo comando na pasta `frontend` para iniciar o frontend.