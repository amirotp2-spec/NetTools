# NetTools

A real, open-source Android network toolkit written in Kotlin with Jetpack Compose and Material 3.
Every tool performs a genuine operation on the device. There are no AI features, no fake results and no private backend.

Package: `com.amir.nettools` · Min SDK 26 · Target/Compile SDK 35 · Persian (RTL) and English UI

## Features

| Tool | How it works |
|------|--------------|
| Ping | Runs the device's own `ping` binary (fixed argument list, no shell) and parses real output |
| DNS Lookup | Own RFC 1035 client over UDP (TCP on truncation); A, AAAA, CNAME, MX, NS, TXT with TTL |
| IP Information | Local analysis of IPv4/IPv6 (scope, normal/expanded form, PTR name) + real reverse DNS; lists this device's IPs |
| HTTP/HTTPS Test | `HttpURLConnection`, manual redirect chain, status, timing, headers, TLS cipher and certificate |
| Internet Connectivity | `ConnectivityManager` capabilities **plus** a real TCP connect to public hosts |
| Network Information | Transport, capabilities, local IPs, gateway, DNS servers, Private DNS; "Unavailable" when Android gives nothing |
| Port Checker | One TCP connection to the single port you enter (no range scanning) |
| Traceroute | System `ping` with increasing TTL; reads real "Time to live exceeded" replies (see limitation below) |
| Speed Test | Download/upload/latency against a Cloudflare-compatible endpoint; only runs when you press Start; approximate |
| Wi-Fi Information | SSID, BSSID, RSSI, link speed, frequency, band (location permission needed for SSID/BSSID) |
| Public IP | ipify (icanhazip fallback) for IPv4 and IPv6, copy and refresh |
| Subnet Calculator | Fully local IPv4/CIDR math, including /31 and /32 |

Also: bottom navigation (Home, Tools, History, Settings), Room-backed history (view / delete / clear), dark / light / system theme, Persian / English language switch.

### Traceroute limitation

Android does not give normal apps raw ICMP sockets. NetTools therefore runs the system `ping` with TTL 1, 2, 3, … and
reads the replies of each router. Latency of intermediate hops is approximate (it includes process start-up time), some routers
never answer (shown as `*`), and devices whose `ping` lacks a TTL option cannot run the tool. The UI explains this, and hops are never invented.

## Installation

1. Open the **Actions** tab of your GitHub repository and run / open the latest **Android CI** workflow.
2. Download the artifact **NetTools-debug-apk**, unzip it and install `app-debug.apk` (allow installs from unknown sources).

## Build instructions

Requirements: JDK 17, Android SDK (platform 35) and Gradle 8.10+.

```bash
gradle testDebugUnitTest --no-daemon
gradle assembleDebug --no-daemon
```

The APK is written to `app/build/outputs/apk/debug/`. No Android Studio is required.
The Gradle wrapper is not committed; run `gradle wrapper --gradle-version 8.10.2` once if you want one.

## GitHub Actions

`.github/workflows/android.yml` checks out the code, sets up Java 17 and Gradle 8.10.2, runs `gradle testDebugUnitTest --no-daemon`,
builds with `gradle assembleDebug --no-daemon` and uploads the APK (and test reports) as artifacts.

## Supported Android versions

Android 8.0 (API 26) and newer. Some details (Private DNS, 6 GHz band) appear only on versions that expose them.

## Architecture

```
com.amir.nettools
├── MainActivity.kt, NetToolsApp.kt     entry point + tiny manual DI container
├── data/local, data/repository         Room database, repository implementation
├── domain/model, domain/repository     UiState, result models, repository interface
├── network/                            Ping, DNS, HTTP, Port, Traceroute, Public IP, Network info, Speed, IP info
├── calculator/SubnetCalculator.kt      pure local math
├── util/                               validators, IP classifier, error mapper, formatting, settings
└── ui/{theme,navigation,components,screens,viewmodel}
```

Every tool ViewModel extends `ToolViewModel`, which exposes `Idle / Loading / Success / Error`, saves successful results to history
and maps exceptions to Persian/English messages. All input is validated before it reaches a socket or a process.

## Privacy

* No analytics, no accounts, no ads and no data sent to any private server.
* Network requests go only to targets you enter, plus the public endpoints used by the tools you run
  (ipify / icanhazip for Public IP, Cloudflare for the speed test, 1.1.1.1 / 8.8.8.8 as DNS fallbacks and reachability probes).
* History is stored only on the device (Room/SQLite) and can be deleted at any time.
* Location permission is requested only on the Wi-Fi screen (Android requires it to reveal the SSID) and the location itself is never read.
* `usesCleartextTraffic` is enabled because the HTTP tester must be able to test plain `http://` URLs.

## License

MIT, see [LICENSE](LICENSE).
