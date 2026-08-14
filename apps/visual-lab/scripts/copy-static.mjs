import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const visualRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const repositoryRoot = resolve(visualRoot, '..', '..')
const dist = resolve(visualRoot, 'dist')
const sceneAssets = resolve(repositoryRoot, 'assets', 'scenes')

await rm(dist, { recursive: true, force: true })
await mkdir(resolve(dist, 'assets'), { recursive: true })
await cp(sceneAssets, resolve(dist, 'assets'), { recursive: true, force: true })
await cp(resolve(visualRoot, 'src', 'style.css'), resolve(dist, 'style.css'), { force: true })

let html = await readFile(resolve(visualRoot, 'index.html'), 'utf8')
html = html.replace('/src/main.ts', './main.js')
html = html.replace('</head>', '  <link rel="stylesheet" href="./style.css" />\n  </head>')
await writeFile(resolve(dist, 'index.html'), html)
