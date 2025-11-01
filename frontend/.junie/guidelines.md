# Frontend Development Guidelines (Shared Finances)

This document captures project-specific practices for the Angular frontend located at `frontend/`. It is written for experienced engineers and focuses only on details specific to this repository.

- Backend repository lives at `../backend` relative to this folder. See `../backend/.junie/guidelines.md` for complementary information (API, auth, DB, and backend runbooks). Frontend/backend are typically run together via the dev proxy detailed below.

## Toolchain overview
- Angular 20 with custom esbuild builder (`@angular-builders/custom-esbuild`).
- TypeScript 5.8, strict mode; module resolution is `bundler` (ESM-first).
- Styling: SCSS + Tailwind v4 + PrimeNG (themes in `@primeng/themes`, `primeng`, `primeicons`).
- i18n: YAML files under `src/i18n` merged at build time by a custom esbuild plugin (`esbuild/i18n-plugin.mjs`) into a virtual module `virtual:i18n-bundle`.
- Lint/format: ESLint + Prettier with import sorting via `@trivago/prettier-plugin-sort-imports`.
- Node ≥ 20 is recommended; Node 22 is known working.

## Build and serve
Scripts in `package.json`:
- `npm start` → `ng serve`
  - Uses the custom esbuild dev server builder (`@angular-builders/custom-esbuild:dev-server`).
  - Dev config is the default for `serve` (`angular.json` → `defaultConfiguration: development`).
  - Proxy is enabled via `proxy.conf.json`.
- `npm run build` → `ng build`
  - Uses `@angular-builders/custom-esbuild:application`.
  - Default configuration is `production` (see `angular.json:projects.shared-finances.architect.build.defaultConfiguration`).
  - Production adds budgets and hashing; development configuration disables optimization and enables source maps.
- `npm run watch` → `ng build --watch --configuration development` (incremental builds without serving).

Important configuration:
- Entry points: `src/main.ts` (browser), polyfills `zone.js` only.
- Assets: everything under `public` is copied to build output under `public/`.
- Styles: single global `src/styles.scss` (component styles are SCSS by default).
- Environments: development replacement maps `src/environments/environment.ts` → `src/environments/environment.development.ts` only for the `development` build configuration.

PrimeNG locale assets:
- The script `npm run copy:prime-i18n` copies JSON locale files from `node_modules/primelocale/*.json` to `public/prime-i18n` using `cpx2`.
- Run this after dependency updates that affect `primelocale` or when adding a new locale.

## Dev proxy and backend integration
`proxy.conf.json`:
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "pathRewrite": { "^/api": "" }
  }
}
```
- At runtime, HTTP calls to `/api/*` from the browser are forwarded to the backend on `localhost:8080` without the `/api` prefix.
- Keep this consistent with the backend’s local port and base path. If the backend changes, update here.
- See `../backend/.junie/guidelines.md` for backend run commands and any required env vars.

## Internationalization (YAML bundling)
- All YAML files under `src/i18n/**` are discovered and merged per-language by `esbuild/i18n-plugin.mjs`.
- The plugin exposes a virtual module `virtual:i18n-bundle` with shape:
  - `export const languages: string[]` → list of language keys inferred from file basenames.
  - `export default { [lang: string]: Record<string, any> }` → merged dictionary per language.
- The plugin watches the YAML files and the i18n directory during dev builds.
- Avoid importing YAML directly; instead import `virtual:i18n-bundle`.

## Linting, formatting, and code style
- Commands:
  - `npm run lint` → runs ESLint (TS + templates) and Prettier check.
  - `npm run lint:fix` → fixes ESLint issues and formats via Prettier.
- Style conventions:
  - Standalone components by default (`@schematics/angular:component` has `standalone: true`).
  - SCSS for styles; prefer component-level encapsulation, global styles restricted to `styles.scss`.
  - Tests are skipped by default in schematics (see `angular.json` `schematics.*.skipTests=true`). If you generate new code with tests, either enable tests explicitly or adopt a repository-wide test framework (see Testing section).
  - Import sorting is handled by Prettier plugin; do not hand-tune ordering.
- TypeScript compiler options are strict (`strict: true`) and ESM-targeted (`target: ES2022`, `module: ES2022`, `moduleResolution: bundler`). Prefer top-level `async/await` where beneficial (supported in Node tooling and modern browsers via bundler).

## Testing
Current status:
- This repository does not include a configured Angular unit test runner (e.g., Karma/Jasmine) nor Jest/Vitest. `ng test` will not work out of the box because the project intentionally uses a custom builder without a test target.

Options:
1) Quick, local script-style tests for utilities using Node’s built-in `node:test` (no deps):
   - Create a temporary file `tmp_test.mjs` with:
     ```js
     import test from 'node:test';
     import assert from 'node:assert/strict';

     function add(a, b) { return a + b }

     await test('add() adds two numbers', () => {
       assert.equal(add(2, 3), 5);
     });
     ```
   - Run: `node tmp_test.mjs`
   - Expected output includes a passing summary (e.g., `pass 1`);
   - Delete the file after running to keep the repo clean.

   Notes:
   - Use this approach only for small, framework-agnostic logic (pure functions, helpers under `src/app/utils` etc.). It’s not suitable for Angular components or DI-heavy code.

2) Adopt a full-featured test runner (recommended for sustained testing):
   - Jest or Vitest are common choices for Angular 15+ with esbuild/Vite-based tooling.
   - If adopting Jest:
     - Use ESM-compatible config (`type: module` or `babel-jest`/`ts-jest` with ESM support).
     - Mock Angular-specific modules as needed; consider `jest-preset-angular` if compatible with the current Angular major.
   - If adopting Vitest:
     - Pair with Vite for component tests or use `@angular/builders` alternatives that integrate.
     - Vitest + `happy-dom` or `jsdom` can cover component tests with minimal friction.
   - In either case, introduce a separate `test` script (e.g., `"test": "vitest"`) and add minimal scaffolding under a `tests/` folder. Keep CI time bounded and leverage `--run`/`--changedSince` for incremental runs.

E2E testing:
- Not configured. Playwright is a modern default. If you add it, prefer component-driven E2E for critical flows and run against `ng serve` with the dev proxy.

## Debugging tips
- When translation keys don’t resolve at runtime, confirm the YAML filenames under `src/i18n/**` are valid and that the plugin merged them (look for the virtual module import usage).
- If dev HTTP requests fail due to CORS, ensure you’re calling `/api/...` (so the proxy applies) and that `../backend` is listening on `localhost:8080`. Update `proxy.conf.json` if the backend port differs.
- For module resolution issues in tooling scripts, remember the repo is ESM-first (`moduleResolution: bundler`). Avoid using CommonJS-only tooling without ESM support.
- After updating `primelocale`, re-run `npm run copy:prime-i18n` to ensure locale JSONs are present under `public/prime-i18n`.

## Typical local workflow
1. Start backend (see `../backend/.junie/guidelines.md`).
2. In this folder:
   - `npm ci`
   - `npm run copy:prime-i18n` (once per locale changes)
   - `npm start` and navigate to `http://localhost:4200`
3. Develop features with standalone components, commit with lint/format clean.
4. For quick validation of utility code, use the temporary `node:test` snippet as shown above, then delete the file.
5. For production build, run `npm run build` (artifacts in `dist/shared-finances`).

## Project structure notes
- Public assets live under `public/` and are emitted under `public/` in the final bundle (keeps relative paths stable).
- i18n YAMLs are colocated under `src/i18n/**` and merged by language key based on filename stem (e.g., `en-US.yaml`, `pt-BR.yaml`). Duplicate keys across files are deep-merged with `deepmerge-ts`.
- Generated models under `src/app/models/generated/**` should not be hand-edited; treat as read-only. Backend generates files here on gradle task `generateTypescriptInterfaces`.

## Adding new code
- Prefer standalone components/services; follow Angular style guide on naming and folder structure.
- Keep services DI-friendly and free of side effects in constructors.
- For DTOs/models, keep them in `models/generated` if sourced from backend codegen; otherwise place domain-specific interfaces in a `models` sibling to avoid codegen collisions.
- Add i18n keys in YAML per language; avoid hardcoded strings in templates.

## Versioning and compatibility
- Angular 20.x and PrimeNG 20.x are expected. When upgrading either, validate the esbuild custom builder compatibility and re-test the i18n plugin. Keep `@angular/compiler` and `@angular/compiler-cli` versions aligned with `@angular/core`.

## What Junie should do before submitting a change
- If you changed any code:
  - Run relevant tests, or the whole test suite if unsure.
  - Ensure `prettier` passes (or run `prettier:fix` first).

---
Last verified: 2025-11-01
- `npm start` and `npm run build` configurations reviewed.
- i18n plugin behavior verified by source inspection.
- Example `node:test` flow executed locally and removed after run.
- Backend proxy settings target `http://localhost:8080` (see backend guidelines for backend run).
