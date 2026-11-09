# Minefiy

A personal Android music player — local library plus YouTube streaming, Material 3 Expressive UI.

This is a private build. It is not distributed or published anywhere.

## Build

One universal APK containing `armeabi-v7a`, `arm64-v8a`, `x86` and `x86_64`:

```bash
./gradlew assembleRelease
```

Result: `app/build/outputs/apk/release/app-release.apk`

The `debug` variant is also built release-grade (R8, resource shrinking, non-debuggable) and
installs alongside the release app under the `....debug` application id:

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
- Together rooms need `TOGETHER_ENDPOINT_SOURCE_URL` in `local.properties` (or the environment);
  when it is empty, online rooms stay offline.

## Credits

Derived from ArchiveTune (GPL-3.0, © Rukamori) and the upstream contributors credited in the
git history and per-file headers.

## License

GPL-3.0 — see [LICENSE](LICENSE).
