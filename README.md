# Minefiy

A personal Android music player — local library plus YouTube streaming, Material 3 Expressive UI.

Forked from [ArchiveTune](https://github.com/rukamori/ArchiveTune) (GPL-3.0) and rebranded by **minetallest**.
This is a private build and is not distributed or published anywhere.

## Build

One universal APK containing `armeabi-v7a`, `arm64-v8a`, `x86` and `x86_64`:

```bash
./gradlew assembleRelease
```

Result: `app/build/outputs/apk/release/app-release.apk`

Debug build (separate `....debug` application id, installs alongside the release):

```bash
./gradlew assembleDebug
```

Requires JDK 21, Android SDK 37 and NDK `30.0.16248370`.

## Notes

- Single variant: `gms` distribution, `mobile` device, universal ABI. There are no flavors.
- The in-app updater is disabled (no GitHub releases are polled).
- The remote data server and gatekeeper are switched off; the app does not contact them.
- Release builds are signed with the debug keystore unless `app/keystore/release.keystore`
  exists together with `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` in the environment.
- Last.fm and Together AI keys are read from `local.properties` or the environment at build time.

## License

GPL-3.0 — see [LICENSE](LICENSE). Original work © Rukamori.
