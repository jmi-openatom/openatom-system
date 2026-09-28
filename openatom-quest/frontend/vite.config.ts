import { fileURLToPath, URL } from 'node:url'
import { execFileSync } from 'node:child_process'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import pkg from './package.json'

let appVersion = `v${pkg.version}.dev`
try {
  const buildNumber = process.env.VITE_BUILD_NUMBER || process.env.GITHUB_RUN_NUMBER
  const commitCount = buildNumber || execFileSync('git', ['rev-list', '--count', 'HEAD']).toString().trim()
  const shortHash = execFileSync('git', ['rev-parse', '--short', 'HEAD']).toString().trim()
  appVersion = `v${pkg.version}.${commitCount}-${shortHash}`
} catch {
  // Local archives without Git metadata still display the package version.
}

export default defineConfig({
  plugins: [vue()],
  define: { __APP_VERSION__: JSON.stringify(process.env.VITE_APP_VERSION || appVersion) },
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    port: 5174,
    proxy: { '/api': { target: 'http://localhost:8931', changeOrigin: true } },
  },
})
