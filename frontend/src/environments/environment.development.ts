import packageJson from '../../package.json';

export const environment = {
  production: false,
  realm: 'shared-finances',
  clientId: 'frontend',
  defaultLanguage: 'en-US',
  defaultPrimeLanguage: 'en',
  singleSsePerBrowser: true,
  /** Always-passes test site key (Cloudflare Turnstile docs). */
  turnstileSiteKey: '1x00000000000000000000AA',
  version: packageJson.version,
};
