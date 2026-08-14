#!/usr/bin/env python3
from __future__ import annotations

from base64 import b64encode
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'apps' / 'visual-lab' / 'dist'


def data_uri(path: Path) -> str:
    return f"data:image/png;base64,{b64encode(path.read_bytes()).decode()}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, default=ROOT / 'GlyphLock-Visual-Lab.html')
    args = parser.parse_args()

    html = (DIST / 'index.html').read_text()
    style = (DIST / 'style.css').read_text()
    layout = (DIST / 'text-layout.js').read_text()
    main_js = (DIST / 'main.js').read_text()

    layout = layout.replace('export function ', 'function ').replace('export {};', '')
    main_js = main_js.replace("import { layoutWithLines, prepareWithSegments } from './text-layout.js';\n", '')
    main_js = main_js.replace("import './style.css';\n", '')

    assets = {
        name: data_uri(DIST / 'assets' / f'scene_{name}.png')
        for name in ('sentinel', 'moth', 'orbit')
    }
    literal = '{' + ','.join(f'{name}:`{uri}`' for name, uri in assets.items()) + '}'
    main_js = main_js.replace('const EVENTS = [', f'const INLINE_ASSETS = {literal};\nconst EVENTS = [', 1)
    main_js = main_js.replace('image.src = `/assets/scene_${id}.png`;', 'image.src = INLINE_ASSETS[id];')

    html = html.replace('    <link rel="stylesheet" href="./style.css" />\n', '')
    html = html.replace('    <script type="module" src="./main.js"></script>\n', '')
    html = html.replace('</head>', f'<style>{style}</style></head>')
    html = html.replace('</body>', f'<script type="module">{layout}\n{main_js}</script></body>')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(html)
    print(args.output)


if __name__ == '__main__':
    main()
