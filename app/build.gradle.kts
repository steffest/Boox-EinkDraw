plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseStoreFilePath = providers.gradleProperty("BOOXDRAW_STORE_FILE")
    .orElse(providers.environmentVariable("BOOXDRAW_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("BOOXDRAW_STORE_PASSWORD")
    .orElse(providers.environmentVariable("BOOXDRAW_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("BOOXDRAW_KEY_ALIAS")
    .orElse(providers.environmentVariable("BOOXDRAW_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("BOOXDRAW_KEY_PASSWORD")
    .orElse(providers.environmentVariable("BOOXDRAW_KEY_PASSWORD"))
val hasReleaseSigning =
    releaseStoreFilePath.isPresent &&
        releaseStorePassword.isPresent &&
        releaseKeyAlias.isPresent &&
        releaseKeyPassword.isPresent

android {
    namespace = "com.boox.einkdraw"
    compileSdk = 34

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFilePath.get())
                storePassword = releaseStorePassword.orNull
                keyAlias = releaseKeyAlias.orNull
                keyPassword = releaseKeyPassword.orNull
            }
        }
    }

    defaultConfig {
        applicationId = "com.boox.einkdraw"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            pickFirsts += "lib/*/libc++_shared.so"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Latest Onyx SDK versions (compatible set)
    implementation(files("libs/onyxsdk-base-1.8.4.aar"))
    implementation(files("libs/onyxsdk-pen-1.5.2.aar"))
    implementation(files("libs/onyxsdk-device-1.3.3.aar"))
    // Native pen engine classes extracted from device knote2 APK (classes7.dex)
    // Provides NeoPenConfig, NeoPenUtils, NeoMarkerPen, NeoPenNative etc. which are
    // missing from the stub onyxsdk-pen-1.5.2.aar but required at runtime by the wrappers.
    implementation(files("libs/onyxsdk-pen-native-classes.jar"))
    
    // RxJava 2 dependencies required by Onyx SDK
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    
    // RxJava 1 fallback (required by some versions of Onyx RxManager)
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("io.reactivex:rxandroid:1.2.1")
    
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    // Fix for ActivityScenario ActivityNotFoundException
    debugImplementation("androidx.test:core:1.6.1")
}

tasks.withType<JavaCompile>().configureEach {
    exclude("**/com/onyx/android/sdk/data/note/TouchPoint.java")
}
