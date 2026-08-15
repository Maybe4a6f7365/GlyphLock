.PHONY: assets checks visual serve capture android clean

assets:
	python3 scripts/generate_scene_masks.py --jobs 3 --compress-level 6

checks:
	python3 scripts/check_morph_first.py
	python3 scripts/check_semantic_composition.py
	python3 scripts/check_transport_integrity.py
	python3 scripts/check_theme_catalog.py

visual: assets checks
	cd apps/visual-lab && npm run build

serve: visual
	cd apps/visual-lab && npm run serve

capture: visual
	cd scripts && python capture_visual_lab.py --theme chrono_loom --event mail --t 1 --output ../preview_reveal.png
	cd scripts && python capture_animation.py --theme chrono_loom --event mail --output ../glyphlock-transform.mp4 --gif ../glyphlock-transform.gif

android: assets checks
	cd apps/android && gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

clean:
	rm -rf apps/visual-lab/dist apps/android/.gradle apps/android/app/build build/review
