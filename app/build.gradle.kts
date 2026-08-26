plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

tasks.register<Exec>("generateKeystore") {
    commandLine("true")
}

android {
    namespace = "com.example.autoclicker"
    compileSdk = 34

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = "upwellclick"
            keyAlias = "upwell-key"
            keyPassword = "upwellclick"
        }
    }

    defaultConfig {
        applicationId = "com.example.autoclicker.v3"
        minSdk = 30
        targetSdk = 34
        versionCode = 9
        versionName = "1.0.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.huawei.hms:ml-computer-vision-ocr:3.18.1.302")
    implementation("com.huawei.hms:ml-computer-vision-ocr-latin-model:3.18.1.302")
}
