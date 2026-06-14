import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
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
  plugins: [
    vue(),
    Components({
      dts: false,
      resolvers: [ElementPlusResolver({ directives: false, importStyle: 'css' })],
    }),
  ],
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
  build: {
    modulePreload: false,
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('mapbox-gl')) return 'mapbox-gl'
          if (id.includes('markdown-it')) return 'markdown'
          if (id.includes('gsap') || id.includes('motion-v')) return 'vendor-motion'
          if (id.includes('/vue/') || id.includes('vue-router')) return 'vendor-vue'
          return undefined
        },
      },
    },
  },
})
