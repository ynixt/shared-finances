[![GitHub license](https://img.shields.io/github/license/ynixt/shared-finances)](https://github.com/ynixt/shared-finances/blob/master/LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/ynixt/shared-finances)](https://github.com/ynixt/shared-finances/releases/latest)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/ynixt/shared-finances/docker-build-main.yml)](https://github.com/ynixt/shared-finances/actions/workflows/docker-build-main.yml)

# Shared Finances

Easily manage your personal and family finances. Free and open source.

# Important notes

> [!CAUTION]  
> We are in alpha version. So be aware of possible bugs.


> [!WARNING]  
> This project was created with help of AI. I know this is a barrier for many people, so please be aware.

# Self Hosted

You can self-host this project using docker compose.
First pick your version at [GitHub Packages](https://github.com/ynixt/shared-finances/pkgs/container/shared-finances).

## All-in-one VS only project

First you need to choose if you just want the **only Shared Finances** or **Shared Finances and their dependencies**.

- Choose **only Shared Finances** if you have experience with Docker and prefers to adjust things your way.
- Choose **Shared Finances and their dependencies** if you want a simpler setup.

### Common configurations

This configuration parameters are common between **only Shared Finances** and **Shared Finances and their dependencies**.

| Parameter                                  | Type              | Required | Default                                                   | Sample                                                      | Description                                                                                                                                               |
|--------------------------------------------|-------------------|----------|-----------------------------------------------------------|-------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SF_APP_POSTGRES_CONNECTION_STRING`        | string            | X        |                                                           | `postgresql://localhost:5432/shared_finances`               | Postgres connection string                                                                                                                                |
| `SF_APP_POSTGRES_USER`                     | string            | X        |                                                           | `shared_finances_user`                                      | Postgres user                                                                                                                                             |
| `SF_APP_POSTGRES_PASSWORD`                 | string            | X        |                                                           | `shared_finances_pass`                                      | Postgres password                                                                                                                                         |
| `SF_APP_REDIS_HOST`                        | string            | X        |                                                           | `localhost`                                                 | Redis host                                                                                                                                                |
| `SF_APP_REDIS_PASSWORD`                    | string            |          |                                                           | `redis_password`                                            | Redis password                                                                                                                                            |
| `SF_APP_REDIS_PORT`                        | number            | X        |                                                           | `6379`                                                      | Redis port                                                                                                                                                |
| `SF_APP_NATS_HOST`                         | string            | X        |                                                           | `localhost`                                                 | Nats host                                                                                                                                                 |
| `SF_APP_NATS_PORT`                         | number            | X        |                                                           | `4222`                                                      | Nats port                                                                                                                                                 |
| `SF_APP_NATS_USER`                         | string            | X        |                                                           | `client_user`                                               | Nats user                                                                                                                                                 |
| `SF_APP_NATS_PASSWORD`                     | string            | X        |                                                           | `client_pass`                                               | Nats password                                                                                                                                             |
| `SF_APP_S3_ENDPOINT`                       | string            | X        |                                                           | `http://localhost:9000`                                     | S3 endpoint                                                                                                                                               |
| `SF_APP_S3_ACCESS_KEY_ID`                  | string            | X        |                                                           | `minioadmin`                                                | S3 access key                                                                                                                                             |
| `SF_APP_S3_SECRET_ACCESS_KEY`              | string            | X        |                                                           | `minioadmin`                                                | S3 secret access key                                                                                                                                      |
| `SF_APP_PORT`                              | number            |          | `80`                                                      |                                                             | Shared Finances HTTP public port (not exists in backend-only image)                                                                                       |
| `SF_APP_API_PORT`                          | number            |          | `8081`                                                    |                                                             | Shared FInances API port  (internal in combined image; exposed in backend-only image; not exists in frontend-only image)                                  |
| `SF_APP_INVITATION_EXPIRATION_MINUTES`     | number            |          | `15`                                                      |                                                             | Time, in minutes, before expiration of group invitation                                                                                                   |
| `SF_APP_JWT_PUBLIC_KEY`                    | string            | X        |                                                           |                                                             | Public key for JWT authentication.<br/>[Click here to learn how to generate this](/docs/generating-jwt-keys.md)                                           |
| `SF_APP_JWT_PRIVATE_KEY`                   | string            | X        |                                                           |                                                             | Private key for JWT authentication.<br/>[Click here to learn how to generate this](/docs/generating-jwt-keys.md)                                          |
| `SF_APP_JWT_KID`                           | number            |          | `1`                                                       |                                                             | Rotation number of JWT key                                                                                                                                |
| `SF_APP_JWT_ISSUER`                        | string            |          |                                                           | `shared_finances`                                           | The JWT issuer name                                                                                                                                       |
| `SF_APP_JWT_ACCESS_TOKEN_TTL_MINUTES`      | number            |          | `15`                                                      |                                                             | Time, in minutes, before user login token expires                                                                                                         |
| `SF_APP_JWT_REFRESH_TOKEN_TTL_MINUTES`     | number            |          | `43200`                                                   |                                                             | Time, in minutes, before user login refresh token expires                                                                                                 |
| `SF_APP_WRONG_PASSWORD_BLOCK`              | number            |          | `10`                                                      |                                                             | How many times user can get your password wrong  before block for `SF_APP_WRONG_PASSWORD_TTL_MINUTES` minutes.                                            |
| `SF_APP_WRONG_PASSWORD_TTL_MINUTES`        | number            |          | `120`                                                     |                                                             | Time, in minutes, that user will cannot try login again after `SF_APP_WRONG_PASSWORD_BLOCK` wrong attempts.                                               |
| `SF_APP_SECURE_COOKIE`                     | boolean           |          | `false`                                                   |                                                             | Defines whether refresh cookie will only be sent by the browser on HTTPS connections.<br>**If you have https please change this to true**                 |
| `SF_APP_MFA_TOTP_CRYPTO_PASSWORD`          | string            |          |                                                           | `changeme`                                                  | Password used to safe store MFA                                                                                                                           |
| `SF_APP_MFA_TOTP_CRYPTO_SALT_HEX`          | string            |          |                                                           | `8e69438719bb1ebbd72c650e1b3262eb`                          | Password-like used to safe store MFA<br>You can generate with `openssl rand -hex 16`                                                                      |
| `SF_APP_SERVICE_SECRET`                    | string            | X        |                                                           | `service-secret`                                            | Password used to authenticate as server                                                                                                                   |
| `SF_APP_PUBLIC_WEB_BASE_URL`               | string            | X        | `http://localhost:4200`                                   |                                                             | Public URL of shared finances. Used on email links.                                                                                                       |
| `SF_APP_TURNSTILE_SECRET_KEY`              | string            |          |                                                           | `1x0000000000000000000000000000000AA`                       | Cloudflare captcha.<br>If you want to use this you'll need to build frontend and change environment `turnstileSiteKey`                                    |
| `SF_APP_TURNSTILE_VERIFY_URL`              | string            |          |                                                           | `https://challenges.cloudflare.com/turnstile/v0/siteverify` |                                                                                                                                                           |
| `SF_APP_EMAIL_CONFIRMATION_ENABLED`        | boolean           |          | `false`                                                   |                                                             | Whether it is needed confirm account via email before login.                                                                                              |
| `SF_APP_PASSWORD_RECOVERY_ENABLED`         | boolean           |          | `false`                                                   |                                                             | Wwhether it is possible to recover the password via email.                                                                                                |
| `SF_APP_TX_MAIL_PROVIDER_PRIORITY`         | list of string    |          |                                                           | `brevo,smtp`                                                | Email servers, separated by commas, should be used to send emails. The further to the left, the higher the priority. If empty, emails will never be sent. |
| `SF_APP_BREVO_DAILY_QUOTA`                 | number            |          | `300`                                                     |                                                             | [Brevo](https://www.brevo.com/) daily quota for sending email. When quota is over is tried the next email server (if exists).                             |
| `SF_APP_BREVO_API_KEY`                     | string            |          |                                                           |                                                             | [Brevo](https://www.brevo.com/) api key                                                                                                                   |
| `SF_APP_BREVO_FROM_ADDRESS`                | string            |          |                                                           | `sample@domain.com`                                         | Email on 'from' used in [Brevo](https://www.brevo.com/)                                                                                                   |
| `SF_APP_BREVO_FROM_NAME`                   | string            |          | `Shared Finances`                                         |                                                             | Name on 'from' used in [Brevo](https://www.brevo.com/)                                                                                                    |
| `SF_APP_SMTP_HOST`                         | string            |          | `localhost`                                               |                                                             | SMTP host                                                                                                                                                 |
| `SF_APP_SMTP_PORT`                         | number            |          | `1025`                                                    |                                                             | SMTP port                                                                                                                                                 |
| `SF_APP_SMTP_USERNAME`                     | string            |          |                                                           |                                                             | SMTP username                                                                                                                                             |
| `SF_APP_SMTP_PASSWORD`                     | string            |          |                                                           |                                                             | SMTP password                                                                                                                                             |
| `SF_APP_SMTP_AUTH`                         | boolean           |          | `false`                                                   |                                                             | If SMTP needs authentication                                                                                                                              |
| `SF_APP_SMTP_STARTTLS`                     | boolean           |          | `false`                                                   |                                                             | SMTP STARTTLS                                                                                                                                             |
| `SF_APP_TX_MAIL_SMTP_FROM`                 | string            |          | `noreply@localhost`                                       |                                                             | Email on 'from' used in SMTP                                                                                                                              |
| `SF_APP_TX_MAIL_SMTP_FROM_NAME`            | string            |          | `Shared Finances`                                         |                                                             | Name on 'from' used in SMTP                                                                                                                               |
| `SF_APP_LEGAL_TERMS_VERSION`               | date (yyyy-mm-dd) |          | `2026-04-14`                                              |                                                             | Date of terms of use                                                                                                                                      |
| `SF_APP_LEGAL_PRIVACY_VERSION`             | date (yyyy-mm-dd) |          | `2026-04-14`                                              |                                                             | Date of terms of privacy                                                                                                                                  |
| `SF_APP_EXCHANGE_RATES_PROVIDER_URL`       | string            |          | `https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@` |                                                             | Provider of exchange rates.<br>Note: for now only [fawazahmed0/exchange-api](https://github.com/fawazahmed0/exchange-api) is supported.                   |
| `SF_APP_REFRESH_EXCHANGE_RATES_CRON`       | string (cron)     |          | `0 0 0/12 * * 1`                                          |                                                             | Cron to get new exchange rates                                                                                                                            |
| `SF_APP_MATERIALIZE_GOAL_CRON`             | string (cron)     |          | `0 0 0 * * *`                                             |                                                             | Cron to generate scheduled financial goals.                                                                                                               |
| `SF_APP_GENERATE_ENTRY_CRON`               | string (cron)     |          | `0 0 0 * * *`                                             |                                                             | Cron to generate scheduled financial transactions.                                                                                                        |
| `SF_APP_EXPIRE_INVITES_CRON`               | string (cron)     |          | `0 0/30 * * * *`                                          |                                                             | Cron to delete expired group invites.                                                                                                                     |
| `SF_APP_UNCONFIRMED_ACCOUNT_CLEANUP_CRON`  | string (cron)     |          | `0 0 0 * * *`                                             |                                                             | Cron to delete not confirmed accounts.                                                                                                                    |
| `SF_APP_SIMULATION_RECONCILE_CRON_ENABLED` | boolean           |          | `false`                                                   |                                                             |                                                                                                                                                           |
| `SF_APP_SIMULATION_RECONCILE_CRON`         | string (cron)     |          | `0 */2 * * * *`                                           |                                                             |                                                                                                                                                           |
| `SF_APP_SIMULATION_PURGE_CRON`             | string (cron)     |          | `0 30 3 * * *`                                            |                                                             | Cron to delete old financial simulations.                                                                                                                 |

### Only Shared Finances Setup

#### Requirements

- Postgres 18+
- Redis 8+
- S3 (you can use minio)
- Nats

### Instalation

1. create `docker-compose.yml` file.

    ```yaml
    name: shared-finances
    
    services:
      shared-finances:
        image: ghcr.io/ynixt/shared-finances:main-20260423-200705-f54e237
        restart: unless-stopped
        stop_grace_period: 45s
        env_file:
          - .env
        environment:
          SF_APP_PORT: ${SF_APP_PORT:-80}
        ports:
          - "${SF_APP_PORT:-80}:${SF_APP_PORT:-80}"
        depends_on:
          db:
            condition: service_healthy
          redis:
            condition: service_healthy
          minio:
            condition: service_healthy
          minio-mc:
            condition: service_completed_successfully
          nats:
            condition: service_healthy
        healthcheck:
          test: ["CMD-SHELL", "curl -fsS http://localhost:$${SF_APP_PORT:-80}/api/open/actuator/health >/dev/null || exit 1"]
          interval: 20s
          timeout: 5s
          retries: 6
          start_period: 20s
    ```
2. Create `.env` file. (remember to change user/password/et.)

   So here's a sample:
    ```dotenv
     ## Postgres
     SF_APP_POSTGRES_CONNECTION_STRING=postgresql://localhost:5432/shared_finances
     SF_APP_POSTGRES_USER=shared_finances_user
     SF_APP_POSTGRES_PASSWORD=shared_finances_pass

     ## Redis
     SF_APP_REDIS_HOST=localhost
     SF_APP_REDIS_PASSWORD=redis_password
     SF_APP_REDIS_PORT=6379

     ## NATS
     SF_APP_NATS_HOST=localhost
     SF_APP_NATS_PORT=4222
     SF_APP_NATS_USER=client_user
     SF_APP_NATS_PASSWORD=client_pass

     ## S3
     SF_APP_S3_ENDPOINT=http://localhost:9000
     SF_APP_S3_ACCESS_KEY_ID=minioadmin
     SF_APP_S3_SECRET_ACCESS_KEY=minioadmin
     SF_APP_S3_REGION=us-east-1
     SF_APP_S3_BUCKET=shared-finances

     ## APP
     SF_APP_INVITATION_EXPIRATION_MINUTES=15
     SF_APP_JWT_PUBLIC_KEY=nice_token_here
     SF_APP_JWT_PRIVATE_KEY=nice_private_token_here
     SF_APP_JWT_KID=k1
     SF_APP_JWT_ISSUER=self_hosted_shared_finances
     SF_APP_JWT_ACCESS_TOKEN_TTL_MINUTES=15
     SF_APP_JWT_REFRESH_TOKEN_TTL_MINUTES=43200
     SF_APP_WRONG_PASSWORD_TTL_MINUTES=120
     SF_APP_MFA_TOTP_CRYPTO_PASSWORD=changeme
     SF_APP_MFA_TOTP_CRYPTO_SALT_HEX=8e69438719bb1ebbd72c650e1b3262eb
     SF_APP_SERVICE_SECRET=service-secret
    ```

3. Run `docker compose up -d`
4. Open in your browser `http://localhost`

### Shared Finances and their dependencies

**Coming soon**

# License

GNU Affero General Public License v3.0 (AGPL-3.0). See [LICENSE](LICENSE).
