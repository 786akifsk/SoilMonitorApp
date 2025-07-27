plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    buildFeatures{
        buildConfig = true
        dataBinding = true
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "SUPABASE_URL", "\"${property("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${property("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "SUPABASE_ROLE_KEY", "\"${property("SUPABASE_ROLE_KEY")}\"")
        buildConfigField("String", "SUPABASE_BUCKET", "\"images\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "src/main/res/assets")
        }
    }
}

dependencies {
    // Networking
    implementation(libs.retrofit) // e.g., retrofit:2.9.0
    implementation(libs.converter.gson) // e.g., converter-gson:2.9.0
    implementation(libs.okhttp) // e.g., okhttp:4.12.0
    implementation(libs.logging.interceptor) // e.g., logging-interceptor:4.12.0
    // Remove libs.okhttp.v4120 to avoid conflict

    // AndroidX and UI
    implementation(libs.appcompat)
    implementation(libs.material.v1110)
    implementation(libs.constraintlayout)
    implementation(libs.drawerlayout)
    implementation(libs.cardview)
    implementation(libs.picasso)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.mik3y:usb-serial-for-android:3.4.6")
    implementation("org.tensorflow:tensorflow-lite:2.9.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.3.1")
    implementation ("com.github.Foysalofficial:NafisBottomNav:5.0")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("com.android.volley:volley:1.2.1")

    implementation("com.github.Dimezis:BlurView:version-1.6.6")
    implementation("com.airbnb.android:lottie:3.4.0")
    implementation("np.com.susanthapa:curved_bottom_navigation:0.7.0")




    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    // JSON Parsing
    implementation(libs.gson)
}
