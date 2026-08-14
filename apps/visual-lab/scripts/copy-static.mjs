import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const dist = resolve(root, 'dist')

await rm(dist, { recursive: true, force: true })
await mkdir(resolve(dist, 'assets'), { recursive: true })
await cp(resolve(root, 'public', 'assets'), resolve(dist, 'assets'), {
  recursive: true,
  force: true,
})
await cp(resolve(root, 'src', 'style.css'), resolve(dist, 'style.css'), {
  force: true,
})

let html = await readFile(resolve(root, 'index.html'), 'utf8')
html = html.replace('/src/main.ts', './main.js')
html = html.replace(
  '</head>',
  '  <link rel="stylesheet" href="./style.css" />\n  </head>',
)
await writeFile(resolve(dist, 'index.html'), html)
