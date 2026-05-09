import { cpSync, existsSync, mkdirSync } from 'fs'
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

for (const targetDir of targets) {
  const parentDir = resolve(targetDir, '..')

  if (targetDir.includes('unpackage') && !existsSync(resolve(projectRoot, 'unpackage/dist/dev/mp-weixin'))) {
    continue
  }

  mkdirSync(parentDir, { recursive: true })
  mkdirSync(targetDir, { recursive: true })
  cpSync(sourceDir, targetDir, { recursive: true })
  syncedCount += 1
  console.log('[sync-vant-weapp] synced to', targetDir)
}

if (syncedCount === 0) {
  console.log('[sync-vant-weapp] no eligible targets found, skipped')
}
