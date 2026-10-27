# Example provenance

Every generated image here was produced by the actual Android app's Java/ONNX Runtime image-to-image pipeline. No desktop diffusion output, cloud generation, mock filters, or image-generation service was substituted.

Recorded on an Android 15 x86_64 emulator with CPU inference, airplane mode enabled, and Wi-Fi disabled. The model is the pinned SD-Turbo + TAESD pack described in the root README. Each example includes its original sketch, one-step preview, and two-step enhanced output. The 768 px sketch is downsampled to 384 px by the real app before inference; generated PNGs are 384 × 384.

| Example | Prompt | Style | Creativity | Seed |
|---|---|---|---|---|
| Cottage | a magical red mushroom cottage, glowing windows, mossy forest, fireflies, cinematic fantasy art | Cinematic | 0.90 | 42 |
| Robot | a small friendly orange robot standing, two glowing cyan eyes, rounded metal body, detailed 3d render, studio lighting | 3D render | 0.85 | 7 |
| Lighthouse | a lighthouse on a rocky island, stormy teal ocean, golden beacon, dramatic clouds, cinematic detailed painting | Cinematic | 0.90 | 42 |

The app appends the selected style suffix to each prompt. The original `*-settings.txt` files record the instrumentation parameters and frame count. Results can vary with runtime, numerical precision, or model changes.

## Capture the real app

The reproducible recorder is [`DemoRecorder.java`](../../app/src/androidTest/java/dev/sketchlive/android/DemoRecorder.java). It draws through the actual sketch View's touch events, presses the app's Generate/Enhance buttons, waits for successful inference, and saves both untouched output PNGs and Android screenshots.

After installing the APK and setting up its model files:

```sh
./gradlew :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class dev.sketchlive.android.DemoRecorder -e demo cottage dev.sketchlive.android.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/dev.sketchlive.android/files/demos/cottage
```

Repeat with `-e demo robot` or `-e demo lighthouse`. Use a test device/emulator: recording changes its sketch and prompt. Capture on a fresh recording directory when regenerating frame sequences to avoid leftover frames from earlier runs.

## Render the comparison GIFs

```sh
python -m pip install pillow
python tools/render_example_gifs.py
```

The comparison renderer requires FFmpeg on your PATH and uses only these checked-in PNGs. It resizes sketches, adds labels, and animates a wipe/crossfade between actual outputs; those transitions are **not inference recordings** and their durations are **not performance measurements**. The separate `android-walkthrough.gif` uses actual Android screenshots, with condensed playback.

App source: [MIT](../../LICENSE). Model weights retain their upstream terms, linked in the root README. ARM phone speed, memory use, and thermal behavior have not been benchmarked.
