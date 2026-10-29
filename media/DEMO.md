# Real Android app recording

Fresh capture of the actual SketchLive Android app in an Android 15 x86_64 emulator, with the real Fast SDXS Sketch model loaded, CPU inference and airplane mode enabled. The existing DemoRecorder instrumentation draws a robot through touch events, taps Generate, waits for a real result and captures the actual Android screen. No stand-in model, result replacement or UI mockup was used.

Prompt: `a small friendly orange robot standing, two glowing cyan eyes, rounded metal body, detailed 3d render, studio lighting`. Style: 3D render. Sketch guidance: 0.85. Seed: 7. The app reported 1.7 s for this warm generation; this is an emulator result, **not physical-phone performance**.

The test completed successfully in 13.645 seconds. Its 39 actual screenshots are paced at five captured frames per second, then converted to a 12 fps GIF with a final hold. Playback is condensed, not a real-time speed measurement; the burned-in caption identifies the emulator/CPU capture.

Reproduce after installing the debug app and instrumentation APK, with the Fast models already downloaded:

```sh
adb shell am instrument -w -r -e class dev.sketchlive.android.DemoRecorder -e demo robot -e mode fast -e recordingPrefix launch-ready-final-v2- dev.sketchlive.android.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/dev.sketchlive.android/files/demos/launch-ready-final-v2-fast-robot
```

The optional recordingPrefix prevents overwriting earlier demo captures. Fast captures now finish on Result; Quality still switches to Sketch before Enhance as before.
