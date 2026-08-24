# Privacy Policy — BarisKeser-ByeDPI

_Last updated: 2026-08-24_

BarisKeser-ByeDPI ("the app") is an open-source tool that runs a **local**
SOCKS5 proxy (ByeDPI) and, in VPN mode, an on-device tunnel to bypass Deep
Packet Inspection (DPI) and censorship. This policy explains what data the app
does and does not handle.

## Data collection

**The app does not collect, store, transmit, or share any personal data.**

- No accounts, no registration, no login.
- No analytics, no advertising, no tracking, no crash-reporting SDKs.
- No data is sent to the developer or to any third-party server by the app.

## How the VPN permission is used

Android's `VpnService` API is used **only** to route your device's traffic
through the local proxy running on your own device (`127.0.0.1`). The app:

- Does **not** send your traffic to any remote VPN server.
- Does **not** encrypt, proxy, log, or inspect the contents of your traffic
  off-device.
- Does **not** hide or change your IP address.

All processing happens locally on your device. The only network connections
made are the ones your own apps initiate to the sites and services you use;
the app merely alters how the initial packets are sent so DPI systems cannot
easily block them.

## Optional DNS setting

In VPN mode you may configure a DNS server (default `1.1.1.1`). If set, DNS
queries from the tunnel are directed to that server. This is a standard public
DNS resolver operated by a third party under its own privacy policy; the app
itself does not receive or store your DNS queries.

## Permissions

- `INTERNET` — to open network connections locally.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — to keep the
  proxy/VPN running while active, with a persistent notification.
- `POST_NOTIFICATIONS` — to show the running-status notification.
- `BIND_VPN_SERVICE` — to provide the on-device VPN tunnel.

## Children

The app is not directed at children and collects no data from anyone.

## Changes

Any changes to this policy will be published in this file in the project
repository.

## Contact

Questions: open an issue at
https://github.com/barkeser2002/ByeDPIAndroid/issues
