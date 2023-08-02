# Shared Finances Frontend

## Before Running

### Requisites

- Node 18+

### Environment variables

Is possible to set the follow env var:

- NG_APP_SERVER_URL - if not defined will assume `localhost:8080`
- NG_APP_USE_SSL - if not defined will assume `false` (this will be used by the websocket)

## Local running

Just call the gradle command `npm install` and then `npm start`.

### Angular proxy

To ensure that no problem with XHR will happen we use angular proxy. 
You can see the configs of the proxy on the file `proxy.conf.json`.

## Helpful docs

- [Angular Docs](https://angular.io/docs)
- [NGRX docs](https://ngrx.io/)
