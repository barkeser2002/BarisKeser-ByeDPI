# Google Play — Release Checklist (BarisKeser-ByeDPI)

Everything the codebase can provide is done. The items marked **[you]** need to
be completed by you in the Play Console / your machine.

## Build / signing (done in this repo)
- `applicationId` = **`com.bariskeser.byedpi`** (permanent once first uploaded to Play).
- `targetSdk` / `compileSdk` = **35** (Play requirement), AGP 8.6.1, Gradle 8.7.
  Edge-to-edge is opted out via `values-v35/themes.xml` so the current UI is unaffected.
- Release **signing** reads the keystore from env/secrets (`app/build.gradle.kts`).
- CI (`.github/workflows/release.yml`) builds a signed **APK** (sideload) and **AAB**
  (Play) on a `v*` tag or manual dispatch, and attaches them to a GitHub Release.

## Produce the upload artifact (.aab)
1. **[you]** Create an upload keystore and add the 4 repo secrets
   (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) — see the
   commands you were given earlier.
2. **[you]** Tag a release:
   ```bash
   git tag v2.0.0 && git push origin v2.0.0
   ```
   The **Release APK** workflow runs and publishes the **`byedpi-release-aab`**
   artifact (and a GitHub Release). Download the `.aab` from the run's Artifacts.
3. **[you]** Enroll in **Play App Signing** when creating the app (recommended):
   Play holds the app-signing key; your keystore is the *upload* key.

## Store listing (provided in `fastlane/metadata/android/`)
- Title: **BarisKeser-ByeDPI** (en-US, tr-TR).
- Short + full descriptions: en-US and tr-TR written.
- App icon 512×512: `app/src/main/ic_launcher-playstore.png` (also `play-assets/icon-512.png`).
- Feature graphic 1024×500: `play-assets/feature-graphic.png` (also in each locale's `images/`).
- **[you]** Phone screenshots: min 2 (1080×1920 or similar). Placeholders exist under
  `fastlane/.../en-US/images/phoneScreenshots/` — replace with real screenshots of the app.

## Policy / declarations **[you]**
- **Privacy policy URL (required for VPN apps):** host `PRIVACY.md` publicly and paste
  the URL in Play Console → Store listing / App content. Easiest: enable GitHub Pages, or
  link the raw file:
  `https://raw.githubusercontent.com/barkeser2002/ByeDPIAndroid/master/PRIVACY.md`
- **Data safety form:** declare **No data collected / no data shared** (matches PRIVACY.md).
- **VpnService policy:** Play scrutinizes VPN apps. Declare that VpnService is the app's
  **core functionality** (on-device DPI-bypass tunnel; no remote VPN server). Be ready to
  explain this in the review notes.
- **Foreground service:** the app uses `FOREGROUND_SERVICE_SPECIAL_USE` (subtype vpn/proxy).
  Play may ask you to justify SPECIAL_USE in the console — describe the local proxy/VPN.
- **Content rating:** complete the questionnaire (utility app, no objectionable content).
- **Target audience:** not directed at children.
- **Ads:** none. **In-app purchases:** none.

## Notes
- Versioning: `versionCode` must increase every upload (currently 11). Bump it per release.
- Keep `applicationId` = `com.bariskeser.byedpi` — it cannot change after the first upload.
- License/attribution: this is a fork of ByeDPIAndroid using ByeDPI (hufrea/byedpi);
  keep upstream attribution (already in README/links). Verify the upstream `LICENSE`
  permits store redistribution before publishing.
