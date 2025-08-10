import type { Plugin, PluginBuild } from 'esbuild';

const defineEnvironmentVars: Plugin = {
  name: 'define-text',
  setup(build: PluginBuild) {
    const options = build.initialOptions;
    const envs: any = {};

    if (process.env) {
      envs['APP_ISSUER_URL'] = process.env['APP_ISSUER_URL'];
    }

    options.define!['envs'] = JSON.stringify(envs);
  },
};

export default [defineEnvironmentVars];
