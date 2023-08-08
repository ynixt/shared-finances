const serverUrl = import.meta.env.NG_APP_SERVER_URL ?? "localhost:8080";
const useSsl = import.meta.env.NG_APP_USE_SSL ?? false;
const websocketUrl = "ws" + (useSsl ? "s" : "") + "://" + serverUrl + "/api/socket";

export const environment = {
  production: true,
  websocketUrl,
  version: require('../../package.json').version,
};
