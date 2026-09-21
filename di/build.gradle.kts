plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.gayadi.android.di"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":domain"))
    implementation(project(":data"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
}
