plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.firebase.crashlytics")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val signingEnvironment = listOf("CIVORA_KEYSTORE_PATH", "CIVORA_KEYSTORE_PASSWORD", "CIVORA_KEY_ALIAS", "CIVORA_KEY_PASSWORD")
val releaseSigningAvailable = signingEnvironment.all { !providers.environmentVariable(it).orNull.isNullOrBlank() }

android {
    namespace = "com.sachinkanna.civora"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.sachinkanna.civora"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"

        val emulatorHost = providers.gradleProperty("civora.emulatorHost").orElse("").get()
        require(emulatorHost.matches(Regex("[A-Za-z0-9.:-]*")))
        resValue("string", "firebase_emulator_host", emulatorHost)
        val isolatedDemo = providers.gradleProperty("civora.emulatorSuite").orElse("").get() == "v2"
        resValue("string", "firebase_emulator_project", if (isolatedDemo) "demo-civora-v2" else "demo-civora")
        resValue("integer", "firebase_emulator_auth_port", if (isolatedDemo) "9100" else "9099")
        resValue("integer", "firebase_emulator_firestore_port", if (isolatedDemo) "8090" else "8080")
        resValue("integer", "firebase_emulator_functions_port", if (isolatedDemo) "5010" else "5001")
        resValue("integer", "firebase_emulator_storage_port", if (isolatedDemo) "9190" else "9199")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningAvailable) create("production") {
            storeFile = file(providers.environmentVariable("CIVORA_KEYSTORE_PATH").get())
            storePassword = providers.environmentVariable("CIVORA_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("CIVORA_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("CIVORA_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        configureEach {
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = providers.gradleProperty("civora.uploadCrashlyticsMapping").orNull == "true"
            }
        }
        release {
            optimization {
                enable = true
            }
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningAvailable) signingConfig = signingConfigs.getByName("production")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.coroutines.play.services)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.crashlytics)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.play.services.location)
    implementation(libs.zxing)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.coroutines.test)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
