import { deepmerge } from 'deepmerge-ts';
import fssync from 'node:fs';
import fs from 'node:fs/promises';
import path from 'node:path';
import YAML from 'yaml';

const I18N_DIR = path.join(process.cwd(), 'src', 'i18n');

async function walk(dir) {
  const out = [];
  const entries = await fs.readdir(dir, { withFileTypes: true });
  for (const e of entries) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...(await walk(p)));
    else out.push(p);
  }
  return out;
}
const isYaml = p => /\.ya?ml$/i.test(p);
const baseNoExt = p => path.basename(p).replace(/\.(ya?ml)$/i, '');

export default {
  name: 'i18n-virtual',
  setup(build) {
    const ns = 'i18n-virtual-ns';

    build.onResolve({ filter: /^virtual:i18n-bundle$/ }, args => ({
      path: args.path,
      namespace: ns,
    }));

    build.onLoad({ filter: /.*/, namespace: ns }, async () => {
      const files = (await walk(I18N_DIR)).filter(isYaml);
      const grouped = new Map();

      for (const file of files) {
        const lang = baseNoExt(file);
        const txt = await fs.readFile(file, 'utf8');
        const parsed = YAML.parse(txt) ?? {};
        if (!grouped.has(lang)) grouped.set(lang, []);
        grouped.get(lang).push(parsed);
      }

      const bundle = {};
      for (const [lang, parts] of grouped) {
        bundle[lang] = parts.reduce((acc, cur) => deepmerge(acc, cur), {});
      }

      const code = `
        // GENERATED MODULE — PLEASE DO NOT EDIT
        export const languages = ${JSON.stringify([...grouped.keys()])};
        const bundle = ${JSON.stringify(bundle)};
        export default bundle;
      `;

      const watchFiles = files;
      const watchDirs = [I18N_DIR].filter(d => fssync.existsSync(d));

      return { contents: code, loader: 'js', watchFiles, watchDirs };
    });
  },
};
