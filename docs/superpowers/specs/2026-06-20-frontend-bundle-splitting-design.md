# Frontend Bundle Splitting Design

## Goal

Reduce the frontend production bundle warning by splitting large vendor dependencies into stable Rollup chunks without changing application behavior.

Current `npm run build` emits a large JavaScript asset around 1 MB after minification. The application is small and mostly contained in `App.vue`, so the first optimization should be build-level chunking rather than UI or routing refactors.

## Scope

In scope:

- Configure Vite/Rollup chunk splitting in `frontend/rule-engine-ui/vite.config.js`.
- Keep application behavior unchanged while allowing import-path-only changes in `src/main.js`, `src/App.vue`, and `src/views/Login.vue`.
- Import Element Plus JavaScript from component subpaths instead of the top-level package so unused components are not bundled.
- Split common dependencies into named chunks:
  - `vue-vendor` for Vue runtime.
  - `element-plus` for Element Plus.
  - `icons` for `lucide-vue-next`.
  - `http` for Axios.
- Verify with `npm run build`.

Out of scope:

- Introducing new dependencies for Element Plus auto-import.
- Refactoring the app into router-based pages.
- Changing UI behavior, API calls, authentication, or backend code.

## Recommended Approach

Use `build.rollupOptions.output.manualChunks` in Vite and replace top-level Element Plus JavaScript imports with component subpath imports. This keeps the dependency set unchanged while avoiding the full Element Plus JavaScript barrel in the production bundle.

Dependency matching should be explicit and simple:

- `vue`, `@vue/*`, and related Vue packages go into `vue-vendor`.
- `element-plus` goes into `element-plus`.
- `lucide-vue-next` goes into `icons`.
- `axios` goes into `http`.
- All other modules use Rollup defaults.

## Alternatives Considered

1. Element Plus on-demand import.
   This can reduce bundle size further, but requires more setup and has higher risk of missing styles or component registration.

2. Route-level lazy loading.
   This is useful for larger apps, but the current frontend does not have a real route structure. Adding one now would be a structural refactor with limited immediate value.

## Verification

Run:

```bash
cd frontend/rule-engine-ui
npm run build
```

Success criteria:

- Build exits with code 0.
- The previous single large app/vendor chunk is split into multiple named assets.
- No Vite chunk-size warning remains for JavaScript assets.
- No source behavior changes are required.

## Risks

- Chunk names may vary slightly if dependency graph changes.
- Total compressed bytes may not drop much; the primary benefit is cacheability and avoiding one oversized initial JavaScript file.
- Vite may still warn if one vendor chunk remains above the default threshold; if that happens, inspect output and split only the oversized dependency further.
