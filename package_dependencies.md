# VoiceNote Application Package Dependencies

## Android Application Dependencies

### Core Dependencies
```kotlin
// Kotlin Coroutines for asynchronous programming
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'

// Jetpack Compose for modern UI
implementation 'androidx.activity:activity-compose:1.7.2'
implementation 'androidx.compose.ui:ui:1.4.3'
implementation 'androidx.compose.ui:ui-tooling-preview:1.4.3'
implementation 'androidx.compose.material3:material3:1.1.1'

// Navigation
implementation 'androidx.navigation:navigation-compose:2.6.0'

// Hilt for Dependency Injection
implementation 'com.google.dagger:hilt-android:2.44'
kapt 'com.google.dagger:hilt-compiler:2.44'
implementation 'androidx.hilt:hilt-navigation-compose:1.0.0'

// Lifecycle
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.1'
implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1'

// Room Database
implementation 'androidx.room:room-runtime:2.5.0'
kapt 'androidx.room:room-compiler:2.5.0'
implementation 'androidx.room:room-ktx:2.5.0'

// DataStore for preferences
implementation 'androidx.datastore:datastore-preferences:1.0.0'
```

### Networking & API
```kotlin
// Retrofit for REST API calls
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// OkHttp for HTTP client
implementation 'com.squareup.okhttp3:okhttp:4.11.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

// WebSocket for real-time communication
implementation 'com.squareup.okhttp3:okhttp-ws:4.11.0'
```

### Audio & Media
```kotlin
// AndroidX Media libraries
implementation 'androidx.media3:media3-common:1.1.1'
implementation 'androidx.media3:media3-exoplayer:1.1.1'
```

### Security
```kotlin
// Biometric authentication
implementation 'androidx.biometric:biometric:1.1.0'

// Crypto utilities
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

### Image Loading
```kotlin
// Coil for image loading
implementation 'io.coil-kt:coil-compose:2.4.0'
```

### Background Processing
```kotlin
// Work Manager for background tasks
implementation 'androidx.work:work-runtime-ktx:2.8.1'

// Job Scheduler alternatives
implementation 'androidx.concurrent:concurrent-futures:1.1.0'
```

### Testing Dependencies
```kotlin
// JUnit for unit testing
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.2'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.2'

// Mockito for mocking
testImplementation 'org.mockito:mockito-core:5.1.1'
testImplementation 'org.mockito.kotlin:mockito-kotlin:4.1.0'

// MockK for Kotlin-specific mocking
testImplementation 'io.mockk:mockk:1.13.4'

// Coroutines Test
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4'

// Espresso for UI testing
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
androidTestImplementation 'androidx.test.espresso:espresso-contrib:3.5.1'
androidTestImplementation 'androidx.test.espresso:espresso-intents:3.5.1'

// AndroidX Test
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test:runner:1.5.2'
androidTestImplementation 'androidx.test:rules:1.5.0'

// Compose UI Tests
androidTestImplementation 'androidx.compose.ui:ui-test-junit4:1.4.3'
debugImplementation 'androidx.compose.ui:ui-tooling:1.4.3'
debugImplementation 'androidx.compose.ui:ui-test-manifest:1.4.3'

// Hilt Testing
testImplementation 'com.google.dagger:hilt-android-testing:2.44'
kaptTest 'com.google.dagger:hilt-compiler:2.44'

androidTestImplementation 'com.google.dagger:hilt-android-testing:2.44'
kaptAndroidTest 'com.google.dagger:hilt-compiler:2.44'
```

### Analytics & Crash Reporting (Optional)
```kotlin
// Firebase (if needed)
implementation 'com.google.firebase:firebase-analytics:21.3.0'
implementation 'com.google.firebase:firebase-crashlytics:18.3.7'

// Alternative analytics
implementation 'com.amplitude:analytics-android:1.12.0'
```

## Backend Dependencies (Node.js/Express)

### Core Server Dependencies
```javascript
{
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "helmet": "^7.0.0",
    "dotenv": "^16.3.1",
    "bcryptjs": "^2.4.3",
    "jsonwebtoken": "^9.0.2",
    "mongoose": "^7.5.0", // or your preferred DB library
    "multer": "^1.4.5-lts.1", // for file uploads
    "uuid": "^9.0.0",
    "axios": "^1.5.0",
    "ws": "^8.13.0", // WebSocket
    "redis": "^4.6.7",
    "rate-limiter-flexible": "^3.0.2",
    "sharp": "^0.32.6", // for image processing if needed
    "ffmpeg-static": "^5.2.0",
    "@ffmpeg/core": "^0.11.1"
  }
}
```

### Development Dependencies
```javascript
{
  "devDependencies": {
    "nodemon": "^3.0.1",
    "jest": "^29.6.2",
    "supertest": "^6.3.3",
    "eslint": "^8.47.0",
    "prettier": "^3.0.2"
  }
}
```

### Audio Processing Dependencies
```javascript
{
  "dependencies": {
    "wav": "^1.0.2",
    "node-wav-player": "^0.1.2",
    "speaker": "^0.5.4",
    "node-record-lpcm16": "^1.0.1",
    "@tensorflow/tfjs-node": "^4.10.0" // if using TensorFlow for VAD
  }
}
```

## Database Dependencies

### PostgreSQL Client (if using PostgreSQL)
```kotlin
// For connecting to PostgreSQL from backend
implementation 'org.postgresql:postgresql:42.6.0'
```

### Redis Client
```kotlin
// For connecting to Redis from backend
implementation 'redis.clients:jedis:4.4.4'
// Or alternatively
implementation 'io.lettuce:lettuce-core:6.2.6'
```

## Build Configuration Dependencies

### Gradle Plugins
```kotlin
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
    id 'kotlin-parcelize'
    id 'kotlinx-serialization' // if using Kotlin serialization
}
```

### Version Catalog (libs.versions.toml)
```toml
[versions]
agp = "8.0.2"
kotlin = "1.8.20"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
lifecycleRuntimeKtx = "2.6.1"
activityCompose = "1.7.2"
composeBom = "2023.06.01"
hiltAndroid = "2.44"
hiltCompiler = "2.44"
hiltNavigationCompose = "1.0.0"
navigationCompose = "2.6.0"
roomRuntime = "2.5.0"
roomCompiler = "2.5.0"
roomKtx = "2.5.0"
datastorePreferences = "1.0.0"
retrofit = "2.9.0"
okhttp = "4.11.0"
coroutines = "1.6.4"
coil = "2.4.0"
workRuntime = "2.8.1"
biometric = "1.1.0"
securityCrypto = "1.1.0-alpha06"
media3 = "1.1.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
dagger-hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hiltAndroid" }
dagger-hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hiltCompiler" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "roomRuntime" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "roomCompiler" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "roomKtx" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coil-kt = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workRuntime" }
androidx-biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
androidx-media3-common = { group = "androidx.media3", name = "media3-common", version.ref = "media3" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
dagger-hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hiltAndroid" }
```

## Environment-Specific Dependencies

### Debug Dependencies
```kotlin
debugImplementation 'androidx.compose.ui:ui-tooling:1.4.3'
debugImplementation 'androidx.compose.ui:ui-test-manifest:1.4.3'
```

### Release Dependencies
```kotlin
// Optimized versions for release builds
releaseImplementation 'com.squareup.leakcanary:leakcanary-android:2.12' // Only if needed for release debugging
```

This comprehensive list covers all the necessary dependencies for the VoiceNote application, including testing frameworks, UI libraries, networking components, security features, and database connectivity. The packages are organized by function and include both Android and backend dependencies where applicable.