# BarisKeser-ByeDPI

[Türkçe](README.md) | **English**

<div align="center">
  <img alt="BarisKeser-ByeDPI logo" src="bariskeser-byedpi-logo.png" width="180" height="180">
</div>

---

Android app that bypasses **DPI (Deep Packet Inspection)** and censorship — with no remote server. It runs the [ByeDPI](https://github.com/hufrea/byedpi) engine (**v0.17.3**) as a local SOCKS5 proxy on your device and, in VPN mode, routes traffic through it using Android's on-device VPN. Nothing is sent to a remote VPN server, your traffic is not proxied off-device, and your IP is not hidden — only the way the first packets of a connection are sent is changed so DPI systems cannot easily block them.

## Features

- **VPN mode** (system-wide) or **local SOCKS5 proxy** mode
- **Turkey ISP presets** — Genel (recommended), Türk Telekom / Kablonet, Turkcell Superonline, Superonline (no-TTL), Vodafone / Mobile
- Advanced **command-line editor** for custom ByeDPI strategies (split, disorder, fake, TTL, TLS record split, and more)
- Configurable DNS and IPv6 in VPN mode
- Quick Settings tile
- English + Turkish UI
- No accounts, no ads, no analytics, no tracking

## Installation

[<img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png"
    alt="Get it on GitHub"
    height="80">](https://github.com/barkeser2002/BarisKeser-ByeDPI/releases)

### Or use Obtainium

1. Install [Obtainium](https://github.com/ImranR98/Obtainium/blob/main/README.md#installation)
2. Add the app by URL:
   `https://github.com/barkeser2002/BarisKeser-ByeDPI`

## Turkey presets

Open **Settings → Türkiye Preseti (ISP)** and pick your provider. The default **Genel** profile is a verified working strategy; if a site is slow or blocked on your ISP, try the ISP-specific presets. These write a ByeDPI command line that you can further tweak in the command-line editor.

## Settings

More about the various options can be found in the [ByeDPI documentation](https://github.com/hufrea/byedpi/blob/v0.17.3/README.md).

## FAQ

**Does the app require root?** No. All features work without root.

**Is this a VPN?** It uses Android's VPN mode to redirect traffic locally, but it does not send anything to a remote server, does not encrypt traffic, and does not hide your IP.

**What data does it collect?** None. All processing happens on your device. See [PRIVACY.md](PRIVACY.md).

## Building

Requirements: JDK 17+, Android SDK, Android NDK, CMake 3.22.1+.

1. Clone with submodules:
   ```bash
   git clone --recurse-submodules https://github.com/barkeser2002/BarisKeser-ByeDPI
   ```
2. Build:
   ```bash
   ./gradlew assembleRelease
   ```
3. The APK will be in `app/build/outputs/apk/release/`.

CI (GitHub Actions) builds a debug APK on every pull request and a signed APK + AAB on every push to `master` and on `v*` tags.

## Dependencies

- [ByeDPI](https://github.com/hufrea/byedpi)
- [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)

## License

GPL-3.0. This is a fork/derivative of ByeDPIAndroid; upstream copyrights are retained.
