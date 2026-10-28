# Releasing SketchLive on Google Play

The app is ready to bundle. The steps below are the parts that need your own accounts, keys, or hardware.

## 1. One-time setup

**Upload key.** Play App Signing holds the real app-signing key; you sign uploads with an upload key. Generate one and keep it backed up outside the repo:

```sh
keytool -genkeypair -v -keystore ~/keys/sketchlive-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
```

Create `keystore.properties` in the repo root. It is gitignored. Alternatively, point `SKETCHLIVE_KEYSTORE_PROPERTIES` at a file elsewhere:

```properties
storeFile=C:/Users/you/keys/sketchlive-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

**Report contact.** Play's AI-generated content policy requires user reports to reach you. Add this to `~/.gradle/gradle.properties`:

```properties
sketchlive.reportEmail=reports@yourdomain.example
```

Release builds fail if either the key or the report contact is missing.

## 2. Build

```sh
./gradlew :core:test :app:lintRelease :app:bundleRelease
```

The upload file is `app/build/outputs/bundle/release/app-release.aab`. Bump `versionCode` in `app/build.gradle` for every upload.

## 3. Play Console answers

| Section | Answer |
|---|---|
| App category | Art & Design |
| Privacy policy URL | `https://github.com/DylanMartinLSC/sketchlive-android/blob/main/PRIVACY.md` |
| Ads | No ads |
| App access | All functionality available without login. Reviewers need to download the Fast model (~1.26 GB) in the app first. |
| Data safety: collected | **None.** Sketches, prompts, and images never leave the device. Optional email reports are user-initiated through the user's own email app. |
| Data safety: shared | None |
| Data safety: encryption in transit | Yes (HTTPS model downloads) |
| Data safety: deletion | Not applicable (no data collected). Uninstalling removes all local data. |
| Target audience | 13+ (or 18+ if you want to be maximally conservative). Not designed for children. |
| Content rating (IARC) | Answer **Yes** to "Does the app generate content using AI?" and "user-generated content". Describe the prompt filter and in-app Report. Expect Teen/PEGI 12. |
| Generative AI disclosure | The app generates images from user prompts with on-device diffusion models. It has prompt safeguards and in-app reporting (Report button and long-press on result). |

**Store listing.** State clearly that it is an AI image generator, it requires a one-time 1.26 GB download (Quality: optional 2.42 GB), and 6 GB+ RAM is recommended. Screenshots must show real app output.

**Device catalog.** Under *Release > Device catalog*, exclude devices with less than 4 GB RAM. On a 2 GB emulator the Fast and Quality models are killed by Android's low-memory killer. The app now warns before downloading and explains the closure afterwards, but excluding those devices avoids bad reviews.

## 4. Testing track (required for new personal developer accounts)

Personal accounts created after November 2023 must run a **closed test with at least 12 testers for 14 consecutive days** before production access. Upload the bundle to a closed track first.

Check the **pre-launch report** after the upload. Its robo crawler won't download a 1.26 GB model, so it only covers the first-run screens.

## 5. Physical-phone checklist (not yet done)

Emulator runs don't predict phone behavior. Before production, on at least one 6 GB and one 8 GB+ ARM phone:

- [ ] Fast download completes on Wi-Fi. Leaving the app and returning resumes it.
- [ ] First and warm Fast generation time. Ten Live updates in a row without a crash.
- [ ] Quality generation and Enhance on the 8 GB phone.
- [ ] 5 minutes of Live drawing: check heat and battery drop, and that the app stays responsive.
- [ ] Rotate, split-screen, and dark/light system theme.
- [ ] TalkBack: every button is announced, and Report is reachable.
- [ ] Blocked prompt shows the "Can't create this" dialog. Report removes the image and blocks the prompt.

## 6. Content-safety maintenance

The prompt filter lives in `core/src/main/java/dev/sketchlive/core/ContentSafety.java` with tests in `ContentSafetyTest`. Review emailed reports regularly. Add terms that slipped through, with a test for each, and ship them in the next update. Play's policy expects reports to inform your filtering.
