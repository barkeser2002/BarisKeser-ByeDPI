# BarisKeser-ByeDPI

**Türkçe** | [English](README-en.md)

<div align="center">
  <img alt="BarisKeser-ByeDPI logo" src="bariskeser-byedpi-logo.png" width="180" height="180">
</div>

---

**DPI (Derin Paket İncelemesi)** ve sansürü — uzak sunucuya gerek olmadan — aşan Android uygulaması. Cihazında [ByeDPI](https://github.com/hufrea/byedpi) motorunu (**v0.17.3**) yerel bir SOCKS5 proxy olarak çalıştırır ve VPN modunda trafiği Android'in cihaz-içi VPN'i üzerinden bu proxy'ye yönlendirir. Hiçbir veri uzak bir VPN sunucusuna gönderilmez, trafik cihaz dışında proxy'lenmez ve IP'niz gizlenmez — yalnızca bağlantının ilk paketlerinin gönderilme biçimi değiştirilir; böylece DPI sistemleri kolayca engelleyemez.

## Özellikler

- **VPN modu** (sistem geneli) veya **yerel SOCKS5 proxy** modu
- **Türkiye operatör presetleri** — Genel (önerilen), Türk Telekom / Kablonet, Turkcell Superonline, Superonline (TTL'siz), Vodafone / Mobil
- Özel ByeDPI stratejileri için gelişmiş **komut satırı düzenleyici** (split, disorder, fake, TTL, TLS record split ve dahası)
- VPN modunda yapılandırılabilir DNS ve IPv6
- Hızlı Ayarlar kutucuğu
- İngilizce + Türkçe arayüz
- Hesap yok, reklam yok, analitik yok, takip yok

## Kurulum

[<img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png"
    alt="GitHub'dan indir"
    height="80">](https://github.com/barkeser2002/BarisKeser-ByeDPI/releases)

### Veya Obtainium ile

1. [Obtainium](https://github.com/ImranR98/Obtainium/blob/main/README.md#installation) kur
2. Uygulamayı URL ile ekle:
   `https://github.com/barkeser2002/BarisKeser-ByeDPI`

## Türkiye presetleri

**Ayarlar → Türkiye Preseti (ISP)** menüsünden operatörünü seç. Varsayılan **Genel** profili doğrulanmış çalışan bir stratejidir; operatöründe bir site yavaş veya engelliyse ISP'ye özel presetleri dene. Bunlar, komut satırı düzenleyicide daha da ince ayar yapabileceğin bir ByeDPI komut satırı yazar.

## Ayarlar

Seçenekler hakkında ayrıntı için [ByeDPI belgeleri](https://github.com/hufrea/byedpi/blob/v0.17.3/README.md).

## SSS

**Root gerekir mi?** Hayır. Tüm özellikler root olmadan çalışır.

**Bu bir VPN mi?** Trafiği yerelde yönlendirmek için Android VPN modunu kullanır; ama uzak bir sunucuya bir şey göndermez, trafiği şifrelemez ve IP'nizi gizlemez.

**Hangi verileri toplar?** Hiçbiri. Tüm işlemler cihazınızda yapılır. Bkz. [PRIVACY.md](PRIVACY.md).

## Derleme

Gereksinimler: JDK 17+, Android SDK, Android NDK, CMake 3.22.1+.

1. Submodule'lerle klonla:
   ```bash
   git clone --recurse-submodules https://github.com/barkeser2002/BarisKeser-ByeDPI
   ```
2. Derle:
   ```bash
   ./gradlew assembleRelease
   ```
3. APK `app/build/outputs/apk/release/` altında olur.

## Bağımlılıklar

- [ByeDPI](https://github.com/hufrea/byedpi)
- [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel)

## Lisans

GPL-3.0. Bu, ByeDPIAndroid'in bir fork/türevidir; upstream telif hakları korunur.
