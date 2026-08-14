# Generated scene assets

The high-resolution scene masks are generated locally by:

```bash
python3 scripts/generate_scene_masks.py
```

Generated PNG files are intentionally not tracked. The script writes them to `assets/scenes/`; the Android build copies them into generated resources and the visual laboratory copies them into `dist/assets/`.
