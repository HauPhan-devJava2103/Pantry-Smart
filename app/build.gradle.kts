import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Đọc API key từ local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "hcmute.edu.vn.pantrysmart"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "hcmute.edu.vn.pantrysmart"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Đưa API key vào BuildConfig
        buildConfigField("String", "GEMINI_API_KEY",
            "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
        buildConfigField("String", "PEXELS_API_KEY",
            "\"${localProperties.getProperty("PEXELS_API_KEY", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.glide)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    // Lottie animation cho loading
    implementation("com.airbnb.android:lottie:6.6.2")
}