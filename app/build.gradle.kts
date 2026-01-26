plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.voicenote"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file("/home/basitdev/StudioProjects/voicenote_keystore.jks")
            storePassword = "aka@gami3397"
            keyAlias = "key1"
            keyPassword = "aka@gami3397"
        }
    }

    defaultConfig {
        applicationId = "com.example.voicenote"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    
    // Hilt
    implementation(libs.hiltAndroid)
    kapt(libs.hiltCompiler)
    implementation(libs.hiltNavigationCompose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.appcompat)
    implementation(libs.viewmodelCompose)
    implementation(libs.materialIconsExtended)
    implementation(libs.navigationCompose)

    implementation(libs.biometric)
    implementation(libs.securityCrypto)
    
    // WorkManager
    implementation(libs.workRuntime)

    // Coil
    implementation(libs.coilCompose)

    // Firebase
    implementation(platform(libs.firebaseBom))
    implementation(libs.firebaseFirestore)
    implementation(libs.firebaseStorage)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)

    // Coroutines
    implementation(libs.coroutinesAndroid)
    implementation(libs.coroutinesPlayServices)
}
