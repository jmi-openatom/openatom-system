import { existsSync, readFileSync } from 'fs'
import { resolve } from 'path'
import { spawn, spawnSync } from 'child_process'

const projectRoot = resolve(import.meta.dirname, '..')

function loadEnv(filepath) {
  const env = {}
  if (!existsSync(filepath)) return env
  const content = readFileSync(filepath, 'utf-8')
  for (const line of content.split('\n')) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const eqIdx = trimmed.indexOf('=')
    if (eqIdx === -1) continue
    const key = trimmed.slice(0, eqIdx).trim()
    const val = trimmed.slice(eqIdx + 1).trim()
    if (key && val) env[key] = val
  }
  return env
}

const dotenvFile = resolve(projectRoot, '.env')
const dotenv = loadEnv(dotenvFile)
const hbuilderRoot = '/Applications/HBuilderX.app/Contents/HBuilderX'
const hbuilderNode = resolve(hbuilderRoot, 'plugins/node/node')
const uniCli = resolve(
  hbuilderRoot,
  'plugins/uniapp-cli-vite/node_modules/@dcloudio/vite-plugin-uni/bin/uni.js'
)
const outputDir = resolve(projectRoot, 'unpackage/dist/dev/mp-weixin')

for (const file of [hbuilderNode, uniCli]) {
  if (!existsSync(file)) {
    console.error('[dev:mp-weixin] required HBuilderX file not found:', file)
    process.exit(1)
  }
}

// vant scripts skipped — project now uses tmui only

const child = spawn(hbuilderNode, [uniCli, '-p', 'mp-weixin'], {
  cwd: projectRoot,
  stdio: 'inherit',
  env: {
    ...process.env,
    ...dotenv,
    NODE_ENV: process.env.NODE_ENV || 'development',
    UNI_PLATFORM: 'mp-weixin',
    HX_APP_ROOT: hbuilderRoot,
    UNI_INPUT_DIR: projectRoot,
    UNI_OUTPUT_DIR: outputDir
  }
})

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal)
  }
  process.exit(code ?? 0)
})
