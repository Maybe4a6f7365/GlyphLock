plugins {
    id("com.android.application")
}

android {
    namespace = "dev.glyphlock.wallpaper"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.glyphlock.wallpaper"
        minSdk = 28
        targetSdk = 36
        versionCode = 8
        versionName = "0.8.0-signature-systems"

        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
