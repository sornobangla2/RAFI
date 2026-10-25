plugins {
    id("com.android.application")
}

android {
    namespace = "com.mrrafi.rafiv2"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.mrrafi.rafiv2"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // OkHttp for real HTTP requests (login + submit)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Jsoup for HTML parsing (CSRF token + response parsing)
    implementation("org.jsoup:jsoup:1.18.1")
}
