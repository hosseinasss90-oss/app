# ساخت APK حسابینو

پروژهٔ اندروید Kotlin + Jetpack Compose + Hilt + Room.

## پیش‌نیاز
- Android Studio Ladybug یا جدیدتر
- JDK 17
- Android SDK 35

## در Android Studio
1. File → Open → پوشهٔ `hesabino`
2. صبر کنید Gradle sync تمام شود
3. Run یا Build → Build Bundle(s) / APK(s) → Build APK(s)

خروجی:
`app/build/outputs/apk/debug/app-debug.apk`

## خط فرمان
```bash
export JAVA_HOME=/path/to/jdk-17
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleDebug
```

نسخه فعلی: `1.0.5` (versionCode 6)
