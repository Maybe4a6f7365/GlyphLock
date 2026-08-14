#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import argparse
import math
import shutil
import subprocess
import tempfile

from playwright.sync_api import sync_playwright

from capture_visual_lab import make_html


def smoothstep(value: float) -> float:
    x = max(0.0, min(1.0, value))
    return x * x * (3.0 - 2.0 * x)


def timeline() -> list[float]:
    values: list[float] = []
    values.extend([0.0] * 8)
    values.extend(smoothstep(i / 27.0) for i in range(28))
    values.extend([1.0] * 12)
    values.extend(1.0 - smoothstep(i / 17.0) for i in range(18))
    values.extend([0.0] * 6)
    return values


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--theme', default='sentinel')
    parser.add_argument('--event', default='mail')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gif', type=Path)
    parser.add_argument('--width', type=int, default=540)
    parser.add_argument('--height', type=int, default=1200)
    parser.add_argument('--fps', type=int, default=20)
    args = parser.parse_args()

    html = make_html(args.theme, args.event, 0.0)
    frames = timeline()
    frame_dir = Path(tempfile.mkdtemp(prefix='glyphlock-frames-'))
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(
                headless=True,
                executable_path='/usr/bin/chromium',
                args=['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'],
            )
            page = browser.new_page(
                viewport={'width': args.width, 'height': args.height},
                device_scale_factor=1,
            )
            page.set_content(html, wait_until='load', timeout=180_000)
            page.wait_for_function("document.documentElement.dataset.ready === 'true'", timeout=180_000)
            for index, t in enumerate(frames):
                page.evaluate("t => window.__glyphlock.setCaptureTime(t)", t)
                page.screenshot(path=str(frame_dir / f'frame-{index:04d}.png'), full_page=False)
            browser.close()

        args.output.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run([
            'ffmpeg', '-y', '-loglevel', 'error',
            '-framerate', str(args.fps),
            '-i', str(frame_dir / 'frame-%04d.png'),
            '-c:v', 'libx264', '-preset', 'slow', '-crf', '18',
            '-pix_fmt', 'yuv420p', '-movflags', '+faststart',
            str(args.output),
        ], check=True)

        if args.gif:
            args.gif.parent.mkdir(parents=True, exist_ok=True)
            palette = frame_dir / 'palette.png'
            subprocess.run([
                'ffmpeg', '-y', '-loglevel', 'error',
                '-framerate', str(args.fps),
                '-i', str(frame_dir / 'frame-%04d.png'),
                '-vf', 'fps=15,scale=480:-1:flags=lanczos,palettegen=max_colors=128',
                str(palette),
            ], check=True)
            subprocess.run([
                'ffmpeg', '-y', '-loglevel', 'error',
                '-framerate', str(args.fps),
                '-i', str(frame_dir / 'frame-%04d.png'),
                '-i', str(palette),
                '-lavfi', 'fps=15,scale=480:-1:flags=lanczos[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=4',
                str(args.gif),
            ], check=True)
    finally:
        shutil.rmtree(frame_dir, ignore_errors=True)


if __name__ == '__main__':
    main()
