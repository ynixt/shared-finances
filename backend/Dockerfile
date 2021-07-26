FROM node:14.16-alpine as builder

ENV NODE_ENV build

USER node
WORKDIR /home/node

COPY . /home/node

RUN npm ci 
RUN npm run build

# ---

FROM node:14.16-alpine

ENV NODE_ENV production

USER node
WORKDIR /home/node

COPY --from=builder /home/node/package*.json /home/node/
COPY --from=builder /home/node/dist/ /home/node/dist/
COPY ./unk-shared-finances-firebase-adminsdk.json /home


ENV GOOGLE_APPLICATION_CREDENTIALS /home/unk-shared-finances-firebase-adminsdk.json

RUN npm ci

CMD ["node", "dist/main"]