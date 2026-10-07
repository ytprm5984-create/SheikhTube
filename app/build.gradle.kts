plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android { namespace = "com.sheikhtube.app"; compileSdk = 35
    defaultConfig { applicationId = "com.sheikhtube.app"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0" }
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
