import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

fun loadEnvironmentProperties(environment: String): Properties {
    val propertiesFile = rootProject.file("config/$environment.properties")
    // Configure other flavors without requiring their private settings. Actual builds are
    // checked below, so a prod APK can never silently use example credentials.
    val source = propertiesFile.takeIf { it.exists() }
        ?: rootProject.file("config/$environment.properties.example")
    return Properties().apply { source.inputStream().use(::load) }
}

fun Properties.requiredString(key: String): String =
    getProperty(key)?.takeIf(String::isNotBlank)
        ?: error("Missing required property: $key")

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val googleWebClientId =
    "6035741280-j8ed9ka462jcvhoc14q7hb8vl26iqk0f.apps.googleusercontent.com"
val defaultGoogleDebugAndroidClientId =
    "6035741280-g9agek5bfnkprhp9ubqklb2ustbjd8ld.apps.googleusercontent.com"
val defaultGoogleReleaseAndroidClientId =
    "6035741280-jedtnq850vigud4osf3ce6223i4abbe4.apps.googleusercontent.com"

fun configuredString(value: String?): String? =
    value?.trim()?.takeIf {
        it.isNotBlank() && !it.startsWith("your_")
    }

fun resolvedGoogleWebClientId(raw: String): String {
    val value = raw.trim()
    return if (
        value.isNotBlank() &&
        !value.startsWith("your_") &&
        value.endsWith(".apps.googleusercontent.com")
    ) {
        value
    } else {
        googleWebClientId
    }
}

fun resolvedAndroidClientId(value: String?, key: String, fallback: String): String =
    configuredString(value)?.also { configuredValue ->
        check(configuredValue.endsWith(".apps.googleusercontent.com")) {
            "$key must be a Google Android OAuth client ID."
        }
    } ?: fallback

fun validateProductionApiBaseUrl(value: String) {
    val normalized = value.trim()
    val domain = Regex("^https://([^/:?#]+)(?::[0-9]+)?(?:/[^?#]*)?/?$")
        .matchEntire(normalized)
        ?.groupValues
        ?.get(1)
    check(!domain.isNullOrBlank() && !domain.matches(Regex("[0-9.]+"))) {
        "Production API_BASE_URL must use HTTPS with a domain name."
    }
}

val devProperties = loadEnvironmentProperties("dev")
val prodProperties = loadEnvironmentProperties("prod")
val prodApiBaseUrl = configuredString(providers.gradleProperty("API_BASE_URL").orNull)
    ?: prodProperties.requiredString("API_BASE_URL")
val googleDebugAndroidClientId = resolvedAndroidClientId(
    providers.gradleProperty("GOOGLE_DEBUG_CLIENT_ID").orNull,
    "GOOGLE_DEBUG_CLIENT_ID",
    defaultGoogleDebugAndroidClientId,
)
val googleReleaseAndroidClientId = resolvedAndroidClientId(
    providers.gradleProperty("GOOGLE_CLIENT_ID").orNull,
    "GOOGLE_CLIENT_ID",
    defaultGoogleReleaseAndroidClientId,
)
check(googleDebugAndroidClientId != googleReleaseAndroidClientId) {
    "Debug and release Google Android OAuth client IDs must be different."
}
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.gayadi.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.doonow.gayadi"
        minSdk = 26
        targetSdk = 36
        versionCode = 16
        versionName = "0.0.16"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "KAKAO_MAP_BASE_URL",
            "https://doonow-dev.gayadi.site".asBuildConfigString(),
        )
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            versionNameSuffix = "-dev"
            buildConfigField(
                "String",
                "API_BASE_URL",
                devProperties.requiredString("API_BASE_URL")
                    .replace("localhost", "10.0.2.2")
                    .replace("127.0.0.1", "10.0.2.2")
                    .asBuildConfigString(),
            )
            buildConfigField("boolean", "DEBUG_LOGGING", devProperties.getProperty("DEBUG_LOGGING", "true"))
            buildConfigField(
                "String",
                "KAKAO_MAP_JAVASCRIPT_SDK",
                devProperties.requiredString("KAKAO_MAP_JAVASCRIPT_SDK").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "KAKAO_NATIVE_SDK",
                devProperties.requiredString("KAKAO_NATIVE_SDK").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                resolvedGoogleWebClientId(
                    devProperties.getProperty("GOOGLE_CLIENT_ID", ""),
                ).asBuildConfigString(),
            )
        }

        create("prod") {
            dimension = "environment"
            buildConfigField(
                "String",
                "API_BASE_URL",
                prodApiBaseUrl.trimEnd('/').asBuildConfigString(),
            )
            buildConfigField("boolean", "DEBUG_LOGGING", prodProperties.getProperty("DEBUG_LOGGING", "false"))
            buildConfigField(
                "String",
                "KAKAO_MAP_JAVASCRIPT_SDK",
                prodProperties.requiredString("KAKAO_MAP_JAVASCRIPT_SDK").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "KAKAO_NATIVE_SDK",
                prodProperties.requiredString("KAKAO_NATIVE_SDK").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                resolvedGoogleWebClientId(
                    prodProperties.getProperty("GOOGLE_CLIENT_ID", ""),
                ).asBuildConfigString(),
            )
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.requiredString("storeFile"))
                storePassword = keystoreProperties.requiredString("storePassword")
                keyAlias = keystoreProperties.requiredString("keyAlias")
                keyPassword = keystoreProperties.requiredString("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "GOOGLE_ANDROID_CLIENT_ID",
                googleDebugAndroidClientId.asBuildConfigString(),
            )
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(
                "String",
                "GOOGLE_ANDROID_CLIENT_ID",
                googleReleaseAndroidClientId.asBuildConfigString(),
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.configureEach {
    if (name.matches(Regex("generate(Dev|Prod).*BuildConfig"))) {
        val environment = if (name.startsWith("generateDev")) "dev" else "prod"
        doFirst {
            check(rootProject.file("config/$environment.properties").exists()) {
                "Missing config/$environment.properties. Copy the matching example and configure it."
            }
            if (environment == "prod") {
                validateProductionApiBaseUrl(prodApiBaseUrl)
            }
        }
    }
    when {
        name.startsWith("generateDev") && name.endsWith("BuildConfig") ->
            inputs.file(rootProject.file("config/dev.properties"))
        name.startsWith("generateProd") && name.endsWith("BuildConfig") ->
            inputs.file(rootProject.file("config/prod.properties"))
    }
}

dependencies {
    implementation(project(":di"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:basicinfo"))
    implementation(project(":feature:survey"))
    implementation(project(":feature:surveyresult"))
    implementation(project(":feature:home"))
    implementation(project(":feature:trip"))
    implementation(project(":feature:mypage"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kakao.share)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    testImplementation(libs.junit)
    androidTestImplementation(project(":data"))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
