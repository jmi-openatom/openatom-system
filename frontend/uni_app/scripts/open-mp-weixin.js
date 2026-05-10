import { existsSync } from 'fs'
import { resolve } from 'path'
import { spawnSync } from 'child_process'

const projectRoot = resolve(import.meta.dirname, '..')
const outputDir = resolve(projectRoot, 'unpackage/dist/dev/mp-weixin')
const devtoolsCli = '/Applications/wechatwebdevtools.app/Contents/MacOS/cli'

if (!existsSync(outputDir)) {
  console.error('[open:mp-weixin] output directory not found:', outputDir)
  console.error('[open:mp-weixin] run `pnpm dev:mp-weixin` once before opening DevTools.')
  process.exit(1)
}

if (!existsSync(devtoolsCli)) {
  console.error('[open:mp-weixin] WeChat DevTools CLI not found:', devtoolsCli)
  process.exit(1)
}

const result = spawnSync(devtoolsCli, ['open', '--project', outputDir], {
  cwd: projectRoot,
  stdio: 'inherit'
})

process.exit(result.status ?? 0)
