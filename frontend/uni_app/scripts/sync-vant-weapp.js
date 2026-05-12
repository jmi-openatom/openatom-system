import { cpSync, existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from 'fs'
import { resolve } from 'path'

const projectRoot = resolve(import.meta.dirname, '..')
const sourceDir = resolve(projectRoot, 'node_modules/@vant/weapp/dist')
const targets = [
  resolve(projectRoot, 'wxcomponents/vant'),
  resolve(projectRoot, 'unpackage/dist/dev/mp-weixin/wxcomponents/vant')
]

if (!existsSync(sourceDir)) {
  console.error('[sync-vant-weapp] source not found:', sourceDir)
  process.exit(1)
}

let syncedCount = 0
let patchedCount = 0

function patchWxssSafeArea(targetDir) {
  const entries = readdirSync(targetDir)

  for (const entry of entries) {
    const fullPath = resolve(targetDir, entry)
    const stat = statSync(fullPath)

    if (stat.isDirectory()) {
      patchWxssSafeArea(fullPath)
      continue
    }

    if (!fullPath.endsWith('.wxss')) continue

    const source = readFileSync(fullPath, 'utf-8')
    const patched = source
      .replace(/constant\(safe-area-inset-(top|bottom)\)/g, '0')
      .replace(/env\(safe-area-inset-(top|bottom)\)/g, '0')

    if (patched !== source) {
      writeFileSync(fullPath, patched, 'utf-8')
      patchedCount += 1
    }
  }
}

for (const targetDir of targets) {
  const parentDir = resolve(targetDir, '..')

  if (targetDir.includes('unpackage') && !existsSync(resolve(projectRoot, 'unpackage/dist/dev/mp-weixin'))) {
    continue
  }

  mkdirSync(parentDir, { recursive: true })
  mkdirSync(targetDir, { recursive: true })
  cpSync(sourceDir, targetDir, { recursive: true })
  patchWxssSafeArea(targetDir)
  syncedCount += 1
  console.log('[sync-vant-weapp] synced to', targetDir)
}

if (syncedCount === 0) {
  console.log('[sync-vant-weapp] no eligible targets found, skipped')
} else {
  console.log('[sync-vant-weapp] patched wxss files:', patchedCount)
}
