# Fast mode: experiment and validation

Fast mode uses **SDXS DreamShaper + its Sketch ControlNet**, at **256 × 256**, one Euler step, with the model's tiny TAESD decoder. Quality mode remains SD-Turbo image-to-image at 384 px. Neither mode requires a server or a paid API.

## Why the ControlNet fallback was used

Plain SDXS DreamShaper was evaluated at 256/320 px and noise strengths 0.85, 0.90, 0.95, and 1.0 using the existing cottage, robot, and lighthouse sketches. It generated usable pictures, but structural adherence was weak: the mushroom cottage became a conventional cottage, and the lighthouse could move to the edge of the image. Intermediate timesteps also gave washed-out results, consistent with a model distilled for one-step generation from noise.

The dedicated Sketch ControlNet was then evaluated at 256, 320, and 384 px, with conditioning strengths 0.6, 0.85, and 1.0. It substantially improved placement and silhouettes. 256 px was chosen to prioritize speed; this remains a quality trade-off. The original SD-Turbo option is retained.

The production Fast pipeline encodes the prompt with SD1 CLIP, samples fixed-seed Gaussian noise at t999, conditions one fused UNet/ControlNet pass on the sketch, takes one Euler step to sigma=0, and decodes with tiny TAESD. It does not run a VAE image encoder. It uses white lines on black: monochrome ink on white is inverted; color/filled sketches use per-channel Sobel outlines. Colors therefore come mainly from the prompt, rather than the original color sketch.

Fast's **Sketch guidance** slider changes ControlNet strength. It is intentionally not Quality's Creativity/noise slider. Fast does not run two denoising steps, because that is not the supported SDXS recipe. Switch to Quality for Enhance.

## Recorded Android output

![Actual Android Fast-mode output compared with the input sketches](fast-mode.gif)

The PNGs in [`fast-examples/`](fast-examples) were captured from the actual Android Activity in an Android 15 x86_64 emulator, with airplane mode on and Wi-Fi disabled. They are 256 px model results; gallery presentation enlarges them for comparison. No desktop diffusion output or mock filter was substituted. `*-settings.txt` records exact prompts, styles, seeds, and guidance. The gallery is not an inference recording; the separate `fast-android-walkthrough.gif` contains actual Android screenshots with condensed playback.

## One emulator timing run

| Mode | First generation after loading | Warm generation |
|---|---:|---:|
| Fast · SDXS Sketch · 256 px | 2.1 s | 0.8 s |
| Quality · SD-Turbo · 384 px | 15.7 s | 4.3 s |

These values are from one **desktop-hosted Android emulator** run. Warm Fast was about **5.4× faster** in that run, including the lower resolution and different conditioning path. This is not an isolated model comparison, a statistical benchmark, or a prediction of Samsung Galaxy/Google Pixel FPS. Phone CPU, memory bandwidth, runtime kernels, thermal throttling, and available RAM may change the result substantially.

Both modes used CPU inference, at most four inference threads, the same robot sketch/prompt/seed 7 and 3D-render style. Quality used Creativity 0.85; Fast used Sketch guidance 0.85. Those sliders control different operations. First-generation times include tokenizer/text/model session loading; warm runs reuse sessions and the prompt embedding. [Original timing text](fast-examples/emulator-timings.txt) and [`FastModeDeviceTest`](../app/src/androidTest/java/dev/sketchlive/android/FastModeDeviceTest.java) provide the run details.

## Checks completed

- APK build and Android lint passed.
- All six JVM tests passed with actual Quality and Fast packs; no real-model tests were skipped in this validation run.
- SD1 EOS padding and punctuation differ correctly from the existing SD2 tokenizer. Tensor shapes are profile-specific.
- A fixed Fast seed reproduces the output; removing the sketch changes the result. Blank/ink/color condition-map cases are tested.
- Android generated 256 px Fast output and 384 px Quality output offline. Mode changes during generation are blocked; Stop followed by restart works. Guidance settings are remembered independently of Quality creativity.
- Fast mode rejects unsupported two-step enhancement. Quality Enhance remains available.
- The production downloader resumed and SHA-256-verified a partial Fast graph from its GitHub release URL. The complete model pack's file sizes/checksums are recorded in the manifest.

## Storage and model provenance

Fast downloads **1,259,991,330 bytes (~1.26 GB)** including notices, versus approximately 2.42 GB for Quality. Adding Sketch ControlNet makes this larger than plain SDXS alone; its stronger sketch conditioning was the reason to include it. Inference still needs extra working memory. No physical-phone RAM/thermal measurements are available, so 8 GB RAM remains the recommendation.

The two downloads and verification markers are independent. Existing Quality files are reused during upgrade. Only one inference engine remains loaded; switching profiles releases the previous sessions. Download sizes in the UI are calculated from each pinned manifest, including license files. The app's SDXS conversion is FP16 weights with FP32 float inputs/outputs, exported as static 256 px ONNX; the source revisions and SHA-256 hashes are in `app/src/main/assets/sdxs-manifest.json`.

Base model: [SDXS DreamShaper](https://huggingface.co/IDKiro/sdxs-512-dreamshaper). Conditioning model: [SDXS Sketch ControlNet](https://huggingface.co/IDKiro/sdxs-512-dreamshaper-sketch). Upstream model terms remain applicable; copies of notices/licenses accompany the APK and model download.
