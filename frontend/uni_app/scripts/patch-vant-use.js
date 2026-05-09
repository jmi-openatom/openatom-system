// Patch @vant/use to polyfill isVNode which is missing from
// uni-app's WeChat Mini Program Vue runtime (@dcloudio/uni-mp-vue)
import { readFileSync, writeFileSync } from 'fs'
import { resolve } from 'path'

const target = resolve(import.meta.dirname, '../node_modules/@vant/use/dist/index.esm.mjs')

try {
  let code = readFileSync(target, 'utf-8')

  // Only patch if isVNode is still being imported from vue
  if (!/import\s*\{[^}]*\bisVNode\b[^}]*\}\s*from\s*"vue"/s.test(code)) {
    console.log('[patch-vant-use] already patched, skipping')
    process.exit(0)
  }

  // Remove isVNode from the vue import (handle both "isVNode," and ", isVNode" patterns)
  code = code.replace(/,\s*isVNode\b/g, '')
  code = code.replace(/\bisVNode\s*,/g, '')

  // Inject polyfill right before the first usage (flattenVNodes function)
  const polyfill =
`function isVNode(value) {
  return value !== null && typeof value === 'object' && value.__v_isVNode === true;
}
`
  code = code.replace(
    /(function flattenVNodes)/,
    polyfill + '\n$1'
  )

  writeFileSync(target, code, 'utf-8')
  console.log('[patch-vant-use] patched successfully')
} catch (e) {
  console.error('[patch-vant-use] failed:', e.message)
  process.exit(1)
}
