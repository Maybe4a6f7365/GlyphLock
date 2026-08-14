plugins {
    id("com.android.application")
}

val generatedGlyphResources = layout.buildDirectory.dir("generated/glyphlock/res")
val syncGlyphAssets by tasks.registering(Copy::class) {
    from(rootProject.file("../../assets/scenes"))
    into(generatedGlyphResources.map { it.dir("drawable-nodpi") })
    include("*.png")
}

android {
    namespace = "dev.glyphlock.wallpaper"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.glyphlock.wallpaper"
        minSdk = 28
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0-prototype"

        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    sourceSets.getByName("main").res.srcDir(generatedGlyphResources)

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

tasks.named("preBuild").configure {
    dependsOn(syncGlyphAssets)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
