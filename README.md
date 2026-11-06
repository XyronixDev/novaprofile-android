# NovaProfile — Isolated Browser Profiles

**NovaProfile** (`com.novaprofiles.app`) is an original multi-profile
browser for Android: each profile is an isolated browsing identity with
its own cookies, proxy, user-agent, language, timezone, screen, WebGL and
Canvas settings. Built from a clean-room analysis spec — own code, own
branding, no third-party code, assets, names or endpoints.

## PRO rule (locked)

**Everything is free.** Unlimited profiles, all features unlocked from
the start. There is no VIP, no subscription, no trial, no billing code
anywhere in this app.

## What v1.0.0 does

- **Profile manager (Room):** create / edit / duplicate / delete /
  restore (trash), folders, search, notes.
- **Per-profile WebView isolation:** each profile gets its own WebView
  data directory (via `WebView.setDataDirectorySuffix("p<id>")` on first
  launch in the process) plus per-profile cookie save/restore on stop and
  launch.
- **Per-profile proxy:** HTTP / SOCKS5 (host, port, user, password) via
  `androidx.webkit.ProxyController`, with a real proxy check (HTTPS fetch
  through the proxy). Users bring their own proxies — none are bundled.
- **Identity settings:** user-agent presets (Chrome Android/Desktop,
  Firefox Android, Safari iOS, Custom) + custom UA, language, timezone,
  screen size, device memory, CPU cores.
- **JS-level fingerprint overrides** (injected on every page start):
  Canvas noise (stable per profile), WebGL vendor/renderer, navigator
  properties, screen, AudioContext, WebRTC disable, Do Not Track.
- **Local export / import** of profiles as a `.novaprofile` zip
  (`profiles.json` inside). No server, no account, no cloud sync.

## Honest limits (read before judging the app)

- **Fingerprint protection is JS-level inside the system WebView.**
  It is NOT a Chromium fork and does NOT patch the rendering engine.
  Advanced fingerprint tests (timing, font enumeration, TLS, HTTP
  headers, real GPS) can still detect the real environment. This is a
  v1 trade-off, stated plainly — never claim GoLogin-level
  undetectability.
- **One profile runs at a time.** The proxy override and cookie store
  are process-wide in the system WebView; the running profile's proxy is
  applied on launch and cleared on stop.
- **Geolocation is not spoofed** in v1 (real GPS mocking needs mock
  location permission + developer options). Timezone is overridden at
  the JS `Intl` level only.
- **Data-dir suffix applies on first WebView creation in the process.**
  If a second profile is launched in the same process, its run falls
  back to cookie-restore isolation and the status line says so.

## Stack

Kotlin 2.4.21 · Jetpack Compose BOM 2026.08.00 / 1.12 · AGP 9.5.0 ·
Gradle 9.7 · JDK 21 · compileSdk 37 · targetSdk 36 · minSdk 26 ·
Room 2.7.1 · DataStore · androidx.webkit ProxyController ·
GitHub Actions release build.

## Build

```bash
./gradlew :app:assembleRelease
```

Release signing reads `NOVAPROFILE_STORE_FILE` / `NOVAPROFILE_STORE_PASSWORD` /
`NOVAPROFILE_KEY_ALIAS` / `NOVAPROFILE_KEY_PASSWORD` from `local.properties`
or environment variables. The keystore is never committed (see `.gitignore`).
CI (`.github/workflows/build.yml`) builds an unsigned APK when no keystore
secret is set; sign it locally before installing.

## License

Original code, MIT-style — free to use, no affiliation with any
existing anti-detect product.
