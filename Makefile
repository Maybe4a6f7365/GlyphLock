.PHONY: assets visual serve capture android clean

assets:
	python3 scripts/generate_scene_masks.py

visual: assets
	cd apps/visual-lab && npm run build

serve: visual
	cd apps/visual-lab && npm run serve

capture: visual
	cd scripts && python capture_visual_lab.py --theme sentinel --event mail --t 1 --output ../preview_reveal.png
	cd scripts && python capture_animation.py --output ../glyphlock-transform.mp4 --gif ../glyphlock-transform.gif

android:
	cd apps/android && gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

clean:
	rm -rf apps/visual-lab/dist apps/android/.gradle apps/android/app/build
