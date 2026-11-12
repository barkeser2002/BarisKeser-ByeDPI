# Privacy Policy — BarisKeser-ByeDPI

_Last updated: 2026-11-13_

**Developer:** BarisKeser-ByeDPI
**Contact:** https://github.com/barkeser2002/BarisKeser-ByeDPI/issues

BarisKeser-ByeDPI ("the app") is an open-source tool that runs a **local**
SOCKS5 proxy (ByeDPI) and, in VPN mode, an on-device tunnel to bypass Deep
Packet Inspection (DPI) and censorship. This policy explains what data the app
does and does not handle.

## Data collection

**The app does not collect, store off-device, transmit, or share any personal data.**

- No accounts, no registration, no login.
- No analytics, no advertising, no tracking, no crash-reporting SDKs.
- No data is sent to the developer or to any third-party server by the app.
- The app has no remote server of its own and does not connect to any remote VPN server.

## How the VPN permission is used

Android's `VpnService` API is used **only** to create an on-device tunnel that
routes your device's traffic to the local proxy running on your own device
(`127.0.0.1`). The app:

- Does **not** send your traffic to any remote VPN server.
- Does **not** encrypt your traffic. The app adds no encryption of its own;
  traffic keeps whatever encryption the sites and services you use already provide (for example TLS).
- Does **not** log, store, or inspect the contents of your traffic off-device.
- Does **not** hide or change your IP address.

All processing happens locally on your device. The only network connections
made are the ones your own apps initiate to the sites and services you use;
the app alters how the initial packets of a connection are sent so DPI systems
cannot easily block them.

## Installed app list (per-app tunneling)

To let you choose which apps use the tunnel, the app reads the list of
installed apps that have a launcher icon. This list is read **only on your
device**, is used only to show you the choices and apply your include/exclude
settings, and is **never transmitted or stored off-device**.

## Optional DNS setting

In VPN mode you may configure a DNS server (default `1.1.1.1`). If set, DNS
queries from the tunnel are directed to that server. This is a standard public
DNS resolver operated by a third party under its own privacy policy; the app
itself does not receive, log, or store your DNS queries.

## Data retention and deletion

- Your settings (for example the selected mode, strategy, DNS server, and the
  app include/exclude list) and any saved logs are stored only on your device.
- Nothing is kept on a server, so there is no server-side copy to request deletion of.
- To remove all app data, clear the app's storage in Android Settings or uninstall the app.

## Security

No personal data is sent to the developer, and processing happens on your
device. Because the app does not transmit data, the security of your data
depends on your device. Keep your device updated and use the app's settings
to control which apps use the tunnel.

## Permissions

- `INTERNET`: to open network connections locally.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`: to keep the
  proxy/VPN running while you have turned it on, with a persistent notification.
- `POST_NOTIFICATIONS`: to show the running-status notification (optional; the app works without it).
- `RECEIVE_BOOT_COMPLETED`: to restart the VPN after a reboot, only if you turn on the auto-connect option.
- Launcher-app query (`<queries>`): to list installed apps for per-app tunneling, as described above. Used only on your device.
- `BIND_VPN_SERVICE`: to provide the on-device VPN tunnel. This permission is held by the system, not by the app.

The app does not request battery-optimization exemptions and does not access
photos, media files, contacts, location, or accounts.

## Children

The app is not directed at children and collects no data from anyone.

## Changes

Any changes to this policy will be published in this file in the project
repository and on the hosted version of this policy.

## Contact

Questions: open an issue at
https://github.com/barkeser2002/BarisKeser-ByeDPI/issues
