# NFC Studio Ultra

Android app for reading, writing, and erasing NFC tags with a neon-styled Jetpack Compose UI.

## Features

- Read NDEF tags in reader mode
- Write `TEXT`, `URL`, `PHONE`, `EMAIL`, `SMS`, `LOCATION`, and `CONTACT` records
- Erase NDEF tags
- Auto-open supported reads such as links, phone, email, SMS, maps, and contact insert
- Show basic tag details like type, writable state, size, and tech list

## Requirements

- Android Studio with a current Android SDK
- Java 17
- An NFC-capable Android device for real device testing

## Local development

1. Open the project in Android Studio.
2. Let Gradle sync and install any missing SDK components.
3. Build with `./gradlew assembleDebug` or from Android Studio.
4. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Release signing

Release signing is loaded from local configuration instead of tracked secrets.

You can provide values through:

- `keystore.properties` in the repo root
- Gradle properties
- Environment variables

Example `keystore.properties`:

```properties
storeFile=nfc-studio-ultra-release.jks
storePassword=your-password
keyAlias=nfcstudioultra
keyPassword=your-password
```

Use [keystore.properties.example](./keystore.properties.example) as a template. The real `keystore.properties` and `*.jks` files are gitignored.

## Notes

- The app currently targets NDEF-oriented workflows.
- History/persistence from the older prototype flow is not wired into the current UI.
- Raw low-level tag operations are not included yet.
