# Google Play — Release Checklist (BarisKeser-ByeDPI)

Everything the codebase can provide is done in the repository. The items marked
**[owner]** must be completed in the Play Console or on your machine. Work through
the sections in order.

## Before upload (gating)

Do not upload until every item here is checked.

- [ ] **[owner] Developer account type (VPN-12).** Play Console → Settings → Developer account.
      A VpnService app needs an **Organization** account with a **D-U-N-S** number. The account
      type is not yet confirmed; check it first, because it decides whether this app can be
      published with VpnService at all.
- [ ] **[owner] Automatic protection stays ON.** Keep app bundle "automatic protection" enabled for
      the upload. "Turn off protection for this release" is a one-release workaround only, and
      protection returns on the next upload.
- [ ] **Release build checks** (merged manifest and bundle):
  - [ ] `minSdk` is **24** (required by Play's automatic protection).
  - [ ] `versionCode` is **14 or higher**, and higher than any upload already rejected. Pass
        `-PappVersionCode=<n>` from CI when you need a different value.
  - [ ] `versionName` is `3.0.0` (or the release name you choose).
  - [ ] Native libraries are aligned for **16 KB** pages (`-Wl,-z,max-page-size=16384` and
        `-Wl,-z,common-page-size=16384` in `app/src/main/jni/Application.mk` and
        `app/src/main/cpp/CMakeLists.txt`). This is enforced from 2027-02-01.
  - [ ] `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and `QUERY_ALL_PACKAGES` are **absent** from the
        merged manifest.
  - [ ] `SUPPORTS_ALWAYS_ON` stays `false`. Changing it requires a new VpnService declaration.
- [ ] **[owner] Privacy policy URL is live** and opens in a private browser window:
      https://barkeser2002.github.io/BarisKeser-ByeDPI/
- [ ] **[owner] Play Console forms are complete** (sections below), and their answers match
      `PRIVACY.md` and this document.

## Build / signing (done in this repo)
- `applicationId` = **`com.bariskeser.byedpi`** (permanent once first uploaded to Play).
- `minSdk` = **24**, `targetSdk` = **36**, `compileSdk` = **36**.
- Native code uses NDK `27.2.12479018` with explicit 16 KB linker flags (see above).
- Edge-to-edge is enabled in the app's activities. The `values-v35` opt-out is removed.
- Release **signing** reads the keystore from env/secrets (`app/build.gradle.kts`).
- CI (`.github/workflows/release.yml`) builds a signed **APK** (sideload) and **AAB**
  (Play) on a `v*` tag or manual dispatch, and attaches them to a GitHub Release.

## Produce the upload artifact (.aab)
1. **[owner]** Create an upload keystore and add the 4 repo secrets
   (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
2. **[owner]** Tag a release only after the "Before upload" list is complete:
   ```bash
   git tag v<version> && git push origin v<version>
   ```
   The **Release APK** workflow runs and publishes the **`byedpi-release-aab`**
   artifact (and a GitHub Release). Download the `.aab` from the run's Artifacts.
3. **[owner]** Enroll in **Play App Signing** when creating the app (recommended):
   Play holds the app-signing key; your keystore is the *upload* key.

## App content forms (Play Console → App content)

### 1. VpnService declaration (VPN-1 to VPN-5, VPN-9 to VPN-11)
- **Core functionality:** an on-device DPI-bypass tunnel. If the Console asks for a category,
  choose **"Network-related tools"**.
- **How the tunnel works:** the VpnService tunnel points to a **local SOCKS5 proxy on
  127.0.0.1** inside the app. There is **no remote VPN server**. The proxy changes how the
  first packets of a connection are sent so DPI cannot block them.
- **Encryption:** do not claim that the app encrypts traffic. The app adds no encryption;
  traffic keeps the encryption of the sites and services it uses (for example TLS).
- **Exemptions:** do not claim any exemption.
- **Data collected or shared:** none. **Monetization or traffic redirection:** no.
- **DNS:** the user can set a DNS server (default 1.1.1.1). State this in the declaration.
- **Split tunnelling:** the user can choose which launcher apps use the tunnel
  (include/exclude). The list is read only on the device.
- **Boot auto-connect:** an optional setting. On boot, the VPN starts only if the user has
  accepted the disclosure and granted VPN permission.
- **Video 1 (≤ 90 s):** open the app, press Connect, the system VPN dialog, the key icon and
  notification, a site loading, then Disconnect.
  URL: **[owner] add after recording**.
- **Video 2 (≤ 90 s):** the in-app VPN disclosure, scrolled through in full, the **Agree** path
  (the VPN starts) and the **Decline** path (the VPN does not start, and proxy mode is offered),
  and how to bring the disclosure back (Settings → VPN disclosure).
  URL: **[owner] add after recording**.
- **Resubmit the declaration whenever VpnService usage changes**, including DNS handling,
  split tunnelling, always-on support, and boot auto-connect.

### 2. Foreground service declaration (FGS-13)
- **Permission:** `FOREGROUND_SERVICE_SPECIAL_USE`, used by both services.
- **Use case:** Other / custom.
- **Description (ByeDpiVpnService):**
  "User-initiated on-device VPN (VpnService) that routes device traffic to a local 127.0.0.1 SOCKS5 proxy, which modifies outgoing packets to bypass ISP deep packet inspection. Started and stopped by the user from the app, Quick Settings tile or notification Stop action, and optionally on boot if the user enabled auto-connect. Runs only while enabled; no remote VPN server is used."
- **Description (ByeDpiProxyService):**
  "User-initiated local SOCKS5 proxy on 127.0.0.1 that modifies outgoing packets to bypass ISP deep packet inspection for apps the user configures to use it. Started and stopped by the user; runs only while enabled."
- **User impact if interrupted:** the tunnel drops, and blocked sites are unreachable immediately.
- **Video:** start from the in-app button, from the Quick Settings tile and from boot auto-connect;
  show the ongoing notification; then press Stop.

### 3. Data safety
- Data collected: **none**. Data shared: **none**. Account creation: **no**.
- Answers must match `PRIVACY.md` and the VpnService declaration. If the app ever starts
  collecting or transmitting data, update the Data safety form, `PRIVACY.md` and the
  hosted policy in the same change.

### 4. Privacy policy URL
- https://barkeser2002.github.io/BarisKeser-ByeDPI/ (served from `docs/index.html`).
- The same policy is in `PRIVACY.md` in the repository.

### 5. Store listing: VpnService sentence (VPN-8)
Put this sentence in the **full description** of every locale (en-US and tr-TR), and in the
README files:

- **en-US:** "This app uses Android's VpnService API to route traffic through a local proxy that
  runs on your device. No traffic is sent to a developer or third-party VPN server, and no
  browsing data is collected."
- **tr-TR:** "Bu uygulama, Android'in VpnService API'sini kullanarak cihazınızdaki yerel bir proxy
  üzerinden trafiği yönlendirir. Hiçbir trafik geliştiriciye veya üçüncü taraf bir VPN sunucusuna
  gönderilmez ve hiçbir tarayıcı verisi toplanmaz."

Optional: tablet screenshots.

### 6. App review notes
- The `<queries>` entry for launcher apps is used only to build the per-app tunneling list. The
  list stays on the device and is never sent anywhere (PV-6).
- The app does not request a battery-optimization exemption. The battery step is optional: it
  only opens the system battery-optimization list, and the user makes the choice there.
- VpnService starts only after the user accepts the in-app disclosure. Leaving the disclosure
  without accepting does not start the VPN.

### 7. Other forms
- **Content rating:** complete the questionnaire (utility app, no objectionable content).
- **Target audience:** not directed at children.
- **Ads:** none. **In-app purchases:** none.
- **Not applicable or already compliant:** targetSdk 36, no `QUERY_ALL_PACKAGES`, no accounts
  (no account deletion flow needed), no `READ_MEDIA_*` permissions, no `onBackPressed` or
  `KEYCODE_BACK` overrides.

## Store listing (provided in `fastlane/metadata/android/`)
- Title: **BarisKeser-ByeDPI** (en-US, tr-TR).
- Short + full descriptions: en-US and tr-TR written. Add the VpnService sentence from section 5.
- App icon 512×512: `app/src/main/ic_launcher-playstore.png` (also `play-assets/icon-512.png`).
- Feature graphic 1024×500: `play-assets/feature-graphic.png` (also in each locale's `images/`).
- **[owner]** Phone screenshots: min 2 (1080×1920 or similar). Placeholders exist under
  `fastlane/.../en-US/images/phoneScreenshots/`. Replace them with real screenshots of the app.

## Release checks (after upload, before rollout)
- [ ] The Play Console bundle explorer shows `minSdk` 24 and the new `versionCode`.
- [ ] Automatic protection is ON for the release.
- [ ] Pre-launch report shows no crash on the first run.
- [ ] Install the release on an Android 15 or newer device with 16 KB pages, and check that it starts.

## Notes
- Versioning: `versionCode` must increase every upload. The current fallback is **14**.
- Keep `applicationId` = `com.bariskeser.byedpi`. It cannot change after the first upload.
- License/attribution: this is a fork of ByeDPIAndroid using ByeDPI (hufrea/byedpi).
  Keep upstream attribution (already in README/links). Verify the upstream `LICENSE`
  permits store redistribution before publishing.
