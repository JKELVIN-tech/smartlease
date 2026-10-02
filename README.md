# SmartLease (Android, native)

Native Jetpack Compose port of the SmartLease Lovable site (Smart Lease Manager).
Screens: Sign in / create account, Home (dashboard with charts), Properties + units,
Tenants, Payments ledger, Messages inbox and per-tenant conversation.

It uses the same Supabase backend as the website (`Api.kt`), so both show the same data.
Colours come from the site's `styles.css` tokens (`Theme.kt`).

## Build
1. Open this folder in Android Studio and let Gradle sync.
2. Run on a device/emulator.
3. Release: create a keystore, copy `keystore.properties.example` to `keystore.properties`,
   fill it in, then `./gradlew bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab`.

`store/icon-512.png` is the Play Console listing icon.
