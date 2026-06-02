# aRiMusic Release Build Guide

## Current Build Configuration

**App Name:** aRiMusic
**Current Version:** 0.6.76
**Version Code:** 89
**Min SDK:** 21
**Target SDK:** 35
**Compile SDK:** 35

## Release Build Instructions

### Prerequisites
- Android SDK 35 installed
- NDK installed
- Java 21 or higher
- Gradle 8.0+
- Signing keystore configured

### Step 1: Update Version Numbers

Edit `composeApp/build.gradle.kts`:

```gradle
defaultConfig {
    applicationId = "it.fast4x.rimusic"
    minSdk = 21
    targetSdk = 35
    versionCode = 90           // Increment from 89
    versionName = "0.6.77"     // Increment from 0.6.76
    ...
}
```

### Step 2: Configure Release Signing

#### Option A: Using Debug Keystore (for testing)
```bash
# Current configuration uses debug signing
# Set in build.gradle.kts:
release {
    signingConfig = signingConfigs.getByName("debug")
}
```

#### Option B: Create Release Keystore (for production)

```bash
# Generate keystore
keytool -genkey -v -keystore rimusic-release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias rimusic-release

# Add to local.properties or build.gradle.kts
KEYSTORE_FILE=rimusic-release.keystore
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=rimusic-release
KEY_PASSWORD=your_password
```

### Step 3: Build Release APK

```bash
# Clean build
./gradlew clean

# Build release APK (universal)
./gradlew assembleRelease

# Build release APK for specific architecture
./gradlew assembleRelease -Parch=arm64-v8a
./gradlew assembleRelease -Parch=armeabi-v7a
./gradlew assembleRelease -Parch=x86
./gradlew assembleRelease -Parch=x86_64
```

### Step 4: Build Release AAB (Android App Bundle)

```bash
# Build AAB for Google Play
./gradlew bundleRelease
```

### Step 5: Output Locations

**APKs:** 
- `composeApp/build/outputs/apk/full/release/rimusic-fullRelease.apk`
- `composeApp/build/outputs/apk/accrescent/release/rimusic-accrescentRelease.apk`

**AAB:**
- `composeApp/build/outputs/bundle/fullRelease/app-full-release.aab`
- `composeApp/build/outputs/bundle/accrescentRelease/app-accrescent-release.aab`

## ProGuard/R8 Obfuscation

Release builds include:
- Minification: **Enabled**
- Resource shrinking: **Enabled**
- ProGuard rules: `proguard-rules.pro`

Ensure sensitive APIs are not stripped:
```proguard
# Keep media3 classes
-keep class androidx.media3.** { *; }

# Keep Room database
-keep class androidx.room.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
```

## Testing the Release Build

```bash
# Test release APK on device
adb install -r composeApp/build/outputs/apk/full/release/rimusic-fullRelease.apk

# Or use bundletool for AAB
bundletool build-apks \
  --bundle=app-full-release.aab \
  --output=app.apks \
  --mode=universal

adb install-multiple app.apks
```

## Release Checklist

- [ ] Update version code and version name
- [ ] Test all major features on release build
- [ ] Verify minification doesn't break functionality
- [ ] Test synchronized lyrics fixes (#6101)
- [ ] Test playback error handling (#6099)
- [ ] Test network connectivity (#6098)
- [ ] Create changelog documenting bug fixes
- [ ] Sign APK/AAB with production keystore
- [ ] Verify APK size and method count
- [ ] Test on multiple Android versions (21-35)
- [ ] Create GitHub release with notes
- [ ] Upload to distribution channels

## Build Variants Available

1. **Full Release** - Complete app with all features
   - Flavor: `full`
   - APK: `rimusic-fullRelease.apk`

2. **Accrescent Release** - Alternative distribution
   - Flavor: `accrescent`
   - APK: `rimusic-accrescentRelease.apk`
   - Modified app name: `RiMusic-Acc`

## Gradle Build Commands

```bash
# Build everything
./gradlew build

# Build release only
./gradlew bundleRelease assembleRelease

# Build with dependency analysis
./gradlew build --profile

# Check ProGuard/R8 impact
./gradlew assembleRelease --info

# Generate build report
./gradlew assembleRelease --build-cache
```

## Release Notes (0.6.77)

### Bug Fixes
- Fixed synchronized lyrics timing synchronization (Issue #6101)
  - Corrected lyric line detection timing
  - Added bounds checking to prevent crashes
  - Improved null safety for empty lyrics
  
- Fixed playback error handling (Issue #6099)
  - Added null safety checks for error causes
  - Improved recovery mechanism for network/HTTP errors
  - Better error logging and messages
  
- Fixed network connectivity detection (Issue #6098)
  - Added null safety for ConnectivityManager
  - Improved initial network state detection
  - Added channel cleanup to prevent memory leaks

### Testing Recommendations

1. **Synchronized Lyrics:**
   - Test with songs containing LRC format lyrics
   - Verify timing accuracy across different song lengths
   - Test with empty/malformed lyrics

2. **Playback:**
   - Test on slow/unstable network connections
   - Verify playback recovery after network errors
   - Test skip on error functionality

3. **Network:**
   - Toggle WiFi/mobile during playback
   - Test on devices with varying network conditions
   - Verify app doesn't crash on network changes
