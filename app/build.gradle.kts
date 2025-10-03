plugins {
    alias(libs.plugins.android.application)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.sailspots"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sailspots"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout) // You had 2.2.0 and 2.2.1, libs is 2.2.1
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)

    // --- Firebase and Google Auth Dependencies (Cleaned Up) ---

    // 1. Import the Firebase Bill of Materials (BoM).
    // This manages the versions for other Firebase libraries.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2")) // Use a specific, recent version

    // 2. Add Firebase Authentication dependency WITHOUT specifying a version.
    // The BoM will select the correct version for you.
    implementation("com.google.firebase:firebase-auth")

    // 3. Add the Credentials and Google ID libraries (only once).
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // --- Testing Dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}