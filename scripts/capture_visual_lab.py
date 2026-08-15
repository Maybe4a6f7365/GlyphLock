#!/usr/bin/env python3
from __future__ import annotations

from base64 import b64encode
from pathlib import Path
import argparse
import re
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'apps' / 'visual-lab' / 'dist'


def data_uri(path: Path) -> str:
    mime = 'image/png'
    return f'data:{mime};base64,{b64encode(path.read_bytes()).decode()}'


def make_html(theme: str, event: str, t: float) -> str:
    html = (DIST / 'index.html').read_text()
    style = (DIST / 'style.css').read_text()
    layout = (DIST / 'text-layout.js').read_text()
    main = (DIST / 'main.js').read_text()

    layout = layout.replace('export function ', 'function ')
    layout = layout.replace('export {};', '')
    main = main.replace("import { layoutWithLines, prepareWithSegments } from './text-layout.js';\n", '')
    main = main.replace("import './style.css';\n", '')

    source = (ROOT / 'apps' / 'visual-lab' / 'src' / 'main.ts').read_text(encoding='utf-8')
    names = tuple(dict.fromkeys(re.findall(r"\{\s*id:\s*'([a-z0-9_]+)'\s*,\s*label:", source)))
    if theme not in names:
        raise ValueError(f'unknown theme {theme!r}; available: {names}')
    selected_asset = data_uri(DIST / 'assets' / f'scene_{theme}.png')
    asset_literal = '{' + ','.join(f'{name}:SELECTED_ASSET' for name in names) + '}'
    main = main.replace('const EVENTS = [', f'const SELECTED_ASSET = `{selected_asset}`;\nconst INLINE_ASSETS = {asset_literal};\nconst EVENTS = [', 1)
    main = main.replace('image.src = `/assets/scene_${id}.png`;', 'image.src = INLINE_ASSETS[id];')
    main = main.replace('const query = new URLSearchParams(location.search);', f"const query = new URLSearchParams('?capture=1&theme={theme}&event={event}&t={t}');")

    html = html.replace('<link rel="stylesheet" href="./style.css" />', '')
    html = html.replace('<script type="module" src="./main.js"></script>', '')
    html = html.replace('</head>', f'<style>{style}</style></head>')
    html = html.replace('</body>', f'<script type="module">{layout}\n{main}</script></body>')
    return html


def capture(theme: str, event: str, t: float, output: Path, width: int, height: int) -> None:
    html = make_html(theme, event, t)
    with sync_playwright() as p:
        browser = p.chromium.launch(
            headless=True,
            executable_path='/usr/bin/chromium',
            args=['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'],
        )
        page = browser.new_page(viewport={'width': width, 'height': height}, device_scale_factor=1)
        page.on('pageerror', lambda error: print(f'pageerror: {error}'))
        page.set_content(html, wait_until='load', timeout=180_000)
        page.wait_for_function("document.documentElement.dataset.ready === 'true'", timeout=180_000)
        page.screenshot(path=str(output), full_page=False)
        browser.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--theme', default='sentinel')
    parser.add_argument('--event', default='mail')
    parser.add_argument('--t', type=float, default=1.0)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--width', type=int, default=1080)
    parser.add_argument('--height', type=int, default=2400)
    args = parser.parse_args()
    capture(args.theme, args.event, args.t, args.output, args.width, args.height)


if __name__ == '__main__':
    main()
