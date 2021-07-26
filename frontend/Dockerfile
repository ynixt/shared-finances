FROM node:14.16-alpine as builder
WORKDIR /shared_finances/frontend
COPY ./package*.json ./
RUN npm install
COPY . .
RUN npm run build

# ---

FROM nginx:alpine
COPY --from=builder /shared_finances/frontend/dist/shared-finances /var/www/shared_finances
COPY ./nginx/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80