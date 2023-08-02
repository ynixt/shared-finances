FROM node:18.12.1-alpine as builder

WORKDIR /usr/src/shared_finances/frontend

COPY package.json ./
COPY package-lock.json ./

RUN npm ci

COPY . .

RUN npm run build

# ---

FROM nginx:stable-alpine3.17

COPY --from=builder /usr/src/shared_finances/frontend/dist/shared-finances /var/www/shared_finances
COPY ./nginx/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
