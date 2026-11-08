plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.fittakip.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fittakip.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.9.1"

        // Google ile giriş için Google Cloud'daki "Web application" OAuth istemci kimliği.
        val webClientId = (project.findProperty("fittakip.googleWebClientId") as String?)
            ?: System.getenv("GOOGLE_WEB_CLIENT_ID") ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
    }

    // Sabit imza: her derlemede aynı SHA-1 → Google girişi çalışır, güncellemeler üstüne kurulur.
    val ksPath = System.getenv("FITTAKIP_KEYSTORE")
    signingConfigs {
        if (ksPath != null && file(ksPath).exists()) {
            create("fixed") {
                storeFile = file(ksPath)
                storePassword = System.getenv("FITTAKIP_KEYSTORE_PASSWORD")
                keyAlias = "fittakip"
                keyPassword = System.getenv("FITTAKIP_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("fixed")?.let { signingConfig = it }
        }
        release {
            // Release: debug sürümden belirgin şekilde daha akıcı (Compose debug modda yavaş çalışır).
            isMinifyEnabled = false
            signingConfigs.findByName("fixed")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Bazı Google kütüphaneleri daha yeni bir Kotlin ile derlenmiş; meta veri sürüm kontrolünü atla
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Google ile giriş (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")   // Drive izni (AuthorizationClient)
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")   // barkod (kamera izni gerekmez)
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")   // fotoğraf yönü
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-ai")   // fotoğraftan kalori: Firebase AI Logic → Gemini

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
