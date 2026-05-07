import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { execSync } from 'node:child_process'
import pkg from './package.json'

// Get version info
let appVersion = `v${pkg.version}`
try {
  const buildNumber = process.env.VITE_BUILD_NUMBER || process.env.GITHUB_RUN_NUMBER
  const commitCount = buildNumber || execSync('git rev-list --count HEAD').toString().trim()
  const shortHash = execSync('git rev-parse --short HEAD').toString().trim()
  appVersion = `v${pkg.version}.${commitCount}-${shortHash}`
} catch (e) {
  appVersion = `v${pkg.version}.dev`
}

export default defineConfig({
  plugins: [vue()],
  define: {
    __APP_VERSION__: JSON.stringify(process.env.VITE_APP_VERSION || appVersion),
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
  },
})
