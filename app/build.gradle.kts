plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.sheikhtube.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sheikhtube.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 21
        versionName = "2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
