import { existsSync } from 'fs'
import { resolve } from 'path'
import { spawn, spawnSync } from 'child_process'

const projectRoot = resolve(import.meta.dirname, '..')
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

for (const script of ['patch-vant-use.js', 'sync-vant-weapp.js']) {
  const result = spawnSync(process.execPath, [resolve(projectRoot, 'scripts', script)], {
    cwd: projectRoot,
    stdio: 'inherit'
  })

  if (result.status !== 0) {
    process.exit(result.status ?? 1)
  }
}

const child = spawn(hbuilderNode, [uniCli, '-p', 'mp-weixin'], {
  cwd: projectRoot,
  stdio: 'inherit',
  env: {
    ...process.env,
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
