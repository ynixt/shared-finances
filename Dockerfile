# syntax=docker/dockerfile:1.7

# ── Build stages ────────────────────────────────────────────────────

FROM node:24-bookworm-slim AS frontend-builder
WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:25-jdk AS backend-builder
WORKDIR /workspace/backend

COPY backend/gradlew backend/build.gradle.kts backend/settings.gradle.kts ./
COPY backend/gradle ./gradle
RUN chmod +x ./gradlew

COPY backend/src ./src
RUN ./gradlew --no-daemon build bootJar

FROM nginx:1.29.8 AS frontend-runtime

ENV SF_APP_PORT=80

RUN rm -f /etc/nginx/conf.d/default.conf
COPY docker/nginx/shared-finances-frontend-only.conf /etc/nginx/templates/default.conf.template

COPY --from=frontend-builder /workspace/frontend/dist/shared-finances/browser/ /usr/share/nginx/html/

EXPOSE ${SF_APP_PORT}

FROM eclipse-temurin:25-jre AS backend-runtime

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ENV SF_APP_API_PORT=8081 \
    JAVA_OPTS=""

WORKDIR /opt/shared-finances

COPY --from=backend-builder /workspace/backend/build/libs/shared-finances-*.jar /tmp/
RUN find /tmp -maxdepth 1 -type f -name '*-plain.jar' -delete \
    && mv /tmp/shared-finances-*.jar /opt/shared-finances/shared-finances.jar

EXPOSE ${SF_APP_API_PORT}
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/shared-finances/shared-finances.jar"]

FROM eclipse-temurin:25-jre AS jre-provider

FROM nginx:1.29.8 AS runtime

ENV SF_APP_PORT=80 \
    SF_APP_API_PORT=8081 \
    JAVA_OPTS=""

# Copy JRE
COPY --from=jre-provider /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/java/openjdk/bin:$PATH"

# Create folders
RUN mkdir -p /opt/shared-finances \
             /etc/nginx/templates \
             /etc/nginx/snippets \
             /run/nginx \
    && rm -f /etc/nginx/conf.d/default.conf

# Nginx configurations
COPY docker/nginx/nginx.conf                              /etc/nginx/nginx.conf
COPY docker/nginx/shared-finances-proxy.conf               /etc/nginx/snippets/shared-finances-proxy.conf
COPY docker/nginx/shared-finances-http.conf.template       /etc/nginx/templates/shared-finances-http.conf.template

# Frontend (build Angular)
COPY --from=frontend-builder /workspace/frontend/dist/shared-finances/browser/ /usr/share/nginx/html/

# Backend JAR
COPY --from=backend-builder /workspace/backend/build/libs/shared-finances-*.jar /tmp/
RUN find /tmp -maxdepth 1 -type f -name '*-plain.jar' -delete \
    && mv /tmp/shared-finances-*.jar /opt/shared-finances/shared-finances.jar

COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

EXPOSE ${SF_APP_PORT}

HEALTHCHECK --interval=30s --timeout=5s --retries=5 \
    CMD curl -fsS "http://localhost:${SF_APP_PORT}/api/open/actuator/health" > /dev/null || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
