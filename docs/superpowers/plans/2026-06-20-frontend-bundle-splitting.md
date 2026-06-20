# Frontend Bundle Splitting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the Vue frontend production bundle into stable vendor chunks so the app no longer emits one oversized JavaScript asset.

**Architecture:** Keep the application source unchanged and configure chunk boundaries at Vite/Rollup build level. The only runtime-impacting change is output file grouping for browser loading and cacheability.

**Tech Stack:** Vite 5, Vue 3, Element Plus, lucide-vue-next, Axios, Rollup manual chunks.

## Global Constraints

- Modify `frontend/rule-engine-ui/vite.config.js`, `frontend/rule-engine-ui/src/main.js`, `frontend/rule-engine-ui/src/App.vue`, and `frontend/rule-engine-ui/src/views/Login.vue`.
- Do not add dependencies.
- Do not change UI behavior, API modules, authentication flow, or backend code.
- Verify with `npm run build` from `frontend/rule-engine-ui`.
- Gitee submission requires a Git repository and a configured Gitee remote.

---

### Task 1: Configure Manual Vendor Chunks And Element Plus Subpath Imports

**Files:**
- Modify: `frontend/rule-engine-ui/vite.config.js`
- Modify: `frontend/rule-engine-ui/src/main.js`
- Modify: `frontend/rule-engine-ui/src/App.vue`
- Modify: `frontend/rule-engine-ui/src/views/Login.vue`
- Test: production build output from `npm run build`

**Interfaces:**
- Consumes: Vite `defineConfig` config object.
- Produces: Rollup output chunks named `vue-vendor`, `element-plus`, `icons`, and `http`.

- [ ] **Step 1: Record current build output**

Run:

```bash
cd frontend/rule-engine-ui
npm run build
```

Expected: build exits 0 and shows a large `assets/index-*.js` file around 1 MB after minification.

- [ ] **Step 2: Add Rollup manual chunk config**

Change `frontend/rule-engine-ui/vite.config.js` to:

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

function vendorChunk(id) {
  if (!id.includes('node_modules')) {
    return undefined
  }
  if (id.includes('/element-plus/') || id.includes('\\element-plus\\')) {
    return 'element-plus'
  }
  if (id.includes('/lucide-vue-next/') || id.includes('\\lucide-vue-next\\')) {
    return 'icons'
  }
  if (id.includes('/axios/') || id.includes('\\axios\\')) {
    return 'http'
  }
  if (
    id.includes('/vue/') ||
    id.includes('\\vue\\') ||
    id.includes('/@vue/') ||
    id.includes('\\@vue\\')
  ) {
    return 'vue-vendor'
  }
  return undefined
}

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: vendorChunk
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/rule-engine': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: Run production build**

If `element-plus-*.js` remains above 500 kB, replace top-level Element Plus imports with subpath imports:

```js
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElCheckbox } from 'element-plus/es/components/checkbox/index.mjs'
import { ElCol } from 'element-plus/es/components/col/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElMenu, ElMenuItem } from 'element-plus/es/components/menu/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElRow } from 'element-plus/es/components/row/index.mjs'
```

Replace `ElMessage` imports with:

```js
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
```

- [ ] **Step 4: Run production build**

Run:

```bash
cd frontend/rule-engine-ui
npm run build
```

Expected: build exits 0, output includes separate assets such as `vue-vendor-*.js`, `element-plus-*.js`, `icons-*.js`, and `http-*.js`, and no JavaScript chunk-size warning remains.

- [ ] **Step 5: Confirm frontend dev server state**

Run:

```powershell
Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
```

Expected: if the existing dev server is still running, it listens on `0.0.0.0:5173`; otherwise restart with `npm run dev -- --port 5173`.

- [ ] **Step 6: Prepare Gitee submission**

Run:

```bash
git status --short
git remote -v
```

Expected in the current workspace: these commands fail until the directory is initialized as a Git repository or moved into an existing repository.

If the user provides a Gitee repository URL, initialize and push with:

```bash
git init
git add .
git commit -m "chore: split frontend vendor bundle"
git remote add origin <GITEE_REPOSITORY_URL>
git push -u origin master
```

If the remote default branch is `main`, use:

```bash
git branch -M main
git push -u origin main
```
