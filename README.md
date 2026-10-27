# SketchLive Android

**Draw a few shapes. Describe the scene. Let your phone paint the details.**

Free, open-source sketch-to-image diffusion that runs locally on Android. No desktop server, account, subscription, paid API, or image uploads. Download the model once, then generate in airplane mode.

[**Download the APK**](https://github.com/DylanMartinLSC/sketchlive-android/releases/tag/v0.1.0) · [Build it yourself](#build) · [Desktop project](https://github.com/DylanMartinLSC/sketchlive-krea-alternative)

Android 10+ · 64-bit · 8 GB RAM recommended · 384 × 384 output · one-time ~2.42 GB model download

## From rough shapes to finished scenes

### Mushroom cottage

![A rough red mushroom cottage sketch becomes a detailed cottage in a mossy forest, generated locally by the Android app](docs/cottage.gif)

### Friendly robot

![A simple orange robot sketch becomes a rounded 3D character with glowing cyan eyes, generated locally by the Android app](docs/robot.gif)

### Stormy lighthouse

![A rough lighthouse sketch becomes a dramatic rocky coastline with teal waves and lightning, generated locally by the Android app](docs/lighthouse.gif)

These comparisons use **real SD-Turbo output from the Android app**, including the one-step preview and two-step Enhance. The transitions are edited for comparison; they do not represent inference speed. Captured in an **Android 15 x86_64 emulator using CPU inference, with airplane mode enabled**. Physical-phone performance has not been measured.

| Example | Style | Creativity | Seed |
|---|---|---|---|
| Mushroom cottage | Cinematic | 0.90 | 42 |
| Friendly robot | 3D render | 0.85 | 7 |
| Stormy lighthouse | Cinematic | 0.90 | 42 |

[Original input/output PNGs, exact prompts, and recording details](docs/examples/README.md)

<details>
<summary>Watch the actual Android interface</summary>

<p><img src="docs/android-walkthrough.gif" width="360" alt="Android app recording showing a mushroom cottage being sketched, generated, and enhanced locally" /></p>

Actual emulator screenshots: drawing → Generate → Result → Enhance. Playback is condensed and is not a phone performance benchmark.

</details>

## Install and use

1. Install the SketchLive APK on a **64-bit Android 10 or newer** phone. Allow installation from your browser or file manager when Android asks. This is a development preview, not a Play Store release.
2. Open the app and tap **Download model**. The one-time HTTPS download is about **2.42 GB**. Use Wi-Fi and allow at least **5 GB free storage**. Downloaded files are pinned to an exact revision and verified with SHA-256. Stop pauses the download; Download resumes it.
3. Enter a prompt, choose a style, and draw with your finger or stylus. Use **Generate** for a one-step preview or **Enhance** for two denoising steps. Tap Sketch/Result to switch views; the latest-result thumbnail stays below the drawing tools.
4. Enable **Live** to generate after a short pause following each stroke. While inference runs, keep drawing: the next generation uses the newest sketch. Turn off Live or tap Stop to stop automatic work.
5. Save PNG exports to **Pictures/SketchLive**, or share them through Android's share sheet. Import an image through Android's file picker, or use a result as the next sketch.

After setup, generation works in airplane mode. The app never sends sketches, prompts, or generated images to a server. The INTERNET permission is for model-file downloads. Model cards open in your browser only when you request them.

## What to expect

- **384 × 384 output**, SD-Turbo + the tiny TAESD encoder/decoder. Live uses one denoising step; Enhance uses two, still with TAESD. It does not use the desktop app's full-VAE enhancement.
- **8 GB RAM or more recommended.** The app uses CPU inference for broad compatibility, not a device-specific NPU SDK. The FP16 weight files are about 2.42 GB; inference also needs working memory. Low-memory phones may be unable to run the model.
- **Live means asynchronous updates, not a promised frame rate.** First inference loads the model. Subsequent generations reuse the diffusion session. Speed, heat, and battery use depend on the device; this is not the desktop RTX 3090 experience.
- Creativity controls the actual diffusion noise level, not an image filter. Lower values follow the sketch more closely; higher values allow more invention. Fixed seeds stabilize results on the same runtime. Java and Python use different Gaussian RNGs, so the same numerical seed does not reproduce desktop pixels.
- Brush, stylus pressure, eraser, size, palette, fill, clear, undo/redo, image import, style presets, seed/reroll, live preview, Stop, Enhance, result reuse, PNG save/share, and an in-session history strip are included.
- The sketch, latest result, and settings are restored after reopening. Up to eight recent results are kept in memory during a session. Export anything you want to keep permanently. Leaving the app cancels generation to avoid unintended background compute. Downloads continue while the process is alive, but are not a background service; interrupted downloads can be resumed.
- Models and private drafts are removed when the app is uninstalled. No analytics or ads are included.

## Build

Install JDK 17+ and Android SDK Platform 35 / Build Tools 35.0.0. Set `ANDROID_HOME` to your SDK directory, or put `sdk.dir=/path/to/sdk` in an untracked `local.properties` file.

```sh
git clone https://github.com/DylanMartinLSC/sketchlive-android.git
cd sketchlive-android
./gradlew :app:assembleDebug :app:lintDebug :core:test
# Windows: .\gradlew.bat :app:assembleDebug :app:lintDebug :core:test
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. The preview is signed with the build machine's Android debug key. Keep the same signing key to install upgrades without uninstalling and losing downloaded models. For distribution, configure your own release signing key; do not commit it.

`core` is a plain Java library shared by the Android app and desktop JVM tests. It implements byte-level CLIP BPE, TAESD preprocessing, SD-Turbo's scaled-linear noise schedule, fixed-seed Gaussian noise, and Euler denoising. The app uses the full ONNX Runtime Android AAR, because these graphs need diffusion operators that aren't included in arbitrary reduced-runtime builds.

### Test real inference on a development computer

The optional integration test runs the **same Java inference code** used on Android against the real models. It writes the input, preview, and enhanced result to `core/build/reports/` for inspection. It requires enough host RAM for CPU inference.

```sh
python tools/download_models.py /path/to/models
./gradlew :core:test -DmodelDir=/path/to/models --rerun-tasks
```

Without `modelDir`, tokenizer/scheduler tests run and the real-model integration test is explicitly skipped. A successful JVM integration test is not an Android hardware performance benchmark.

### Validation for this preview

- APK build and Android lint completed successfully.
- JVM tests checked CLIP token IDs against the upstream tokenizer (including accented text/emoji), scheduler sigma values against the pinned manifest, fixed-seed noise, real-model preview, real-model two-step enhancement, and cancellation.
- Android 15 x86_64 emulator tests exercised a resumed HTTPS model download and SHA-256 verification, drawing/fill/undo/redo, actual image generation through the Activity with airplane mode enabled and Wi-Fi disabled, Sketch/Result switching, and successful PNG export to MediaStore.
- ARM64 native libraries are included in the APK. **Physical ARM Android phone speed, memory use, and thermal behavior have not been measured.** The emulator runs on a desktop CPU and does not predict phone frame rates.

The optional Android integration tests in `app/src/androidTest` expect the verified 384 px model pack in the test app's `no_backup/sd-turbo-v1` directory and the integration input at `files/fixture.png`. A verified pack has the `verified-30015d1ff375a67d2ade66f9a43339adf5cd5639` marker written by the downloader. Tests requiring models explicitly skip when setup is incomplete. Run the network download test online; run offline generation in airplane mode after setup. Gradle's connected-test cleanup may uninstall the target and remove its private models, so developers testing multiple stages should install the test APK and use `adb shell am instrument` directly to preserve setup.

## Model provenance and licenses

App source: [MIT](LICENSE). ONNX Runtime: [MIT](https://github.com/microsoft/onnxruntime/blob/main/LICENSE). Gson: [Apache 2.0](https://github.com/google/gson/blob/main/LICENSE). TAESD: [MIT](https://huggingface.co/madebyollin/taesd). SD-Turbo weights retain [Stability AI's model license](https://huggingface.co/stabilityai/sd-turbo/blob/main/LICENSE); downloading the conversion does not grant extra rights to the weights.

The pinned Android conversion is [`xay2001/sd-turbo-android`](https://huggingface.co/xay2001/sd-turbo-android), revision `30015d1ff375a67d2ade66f9a43339adf5cd5639`, folder `sd-turbo-v1`. Its manifest records exact lengths and SHA-256 hashes. Only the 384 px graphs and their shared weight files are downloaded. The CLIP vocabulary/merges come from SD-Turbo revision `b261bac6fd2cf515557d5d0707481eafa0485ec2`; token IDs use SD2's PAD=0 convention.

The app deliberately does not bundle or redistribute model weights inside its APK. Setup links to the upstream model license before download.
