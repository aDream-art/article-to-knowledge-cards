/**
 * 构建入口（无额外依赖）。
 *
 * 为什么不用 `vite build` 直接作为 npm script：
 * 在部分 Windows 环境（受限进程令牌 / 杀软占用）下，esbuild 会把自己在
 * `%TEMP%` 里生成的临时文件删除失败，构建报 `Access is denied`。
 * 把 TMP/TEMP 指到项目内的 `.tmp/` 目录即可稳定规避，同时不影响其他平台。
 */

import { spawn } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const tmpDir = join(root, '.tmp')
mkdirSync(tmpDir, { recursive: true })

const viteBin = join(root, 'node_modules', 'vite', 'bin', 'vite.js')
const child = spawn(process.execPath, [viteBin, 'build', ...process.argv.slice(2)], {
  cwd: root,
  env: { ...process.env, TEMP: tmpDir, TMP: tmpDir },
  stdio: 'inherit',
})

child.on('error', (error) => {
  console.error(`[build] 启动 vite 失败: ${error.message}`)
  process.exit(1)
})

child.on('close', (code, signal) => {
  if (signal) {
    console.error(`[build] vite 被信号 ${signal} 终止`)
    process.exit(1)
  }
  process.exit(code ?? 1)
})
