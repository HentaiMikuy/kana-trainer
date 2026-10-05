import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Kotlin support is built into AGP 9 (no kotlin-android plugin).
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val releaseVersionCode = providers.gradleProperty("ciVersionCode")
    .orElse(appVersion.getProperty("versionCode")).get().toInt().also {
        require(it in 1..2_100_000_000) { "versionCode is outside the Android range" }
    }
val updateRepository = providers.gradleProperty("updateRepository")
    .orElse("HentaiMikuy/kana-trainer").get().also {
        require(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+").matches(it))
    }
val signingVariables = listOf("ANDROID_KEYSTORE_PATH", "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD")
val releaseSigning = signingVariables.associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = releaseSigning.values.all { !it.isNullOrBlank() }

android {
    namespace = "com.konomip.kanatrainer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.konomip.kanatrainer"
        minSdk = 26
        targetSdk = 37
        versionCode = releaseVersionCode
        versionName = appVersion.getProperty("versionName")
        buildConfigField("String", "UPDATE_REPOSITORY", "\"$updateRepository\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("production") {
                storeFile = file(releaseSigning.getValue("ANDROID_KEYSTORE_PATH")!!)
                storePassword = releaseSigning.getValue("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = releaseSigning.getValue("ANDROID_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("production")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // 本机性能验证包：与 debug 包使用同一签名，可覆盖安装；不用于商店发布。
        create("performance") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Fail closed: a release without the permanent signing key must never be published.
val checkProductionSigning = tasks.register("checkProductionSigning") {
    doLast {
        check(hasReleaseSigning) {
            "Release signing is missing. Set " + signingVariables.joinToString() +
                ". See RELEASE.md. Use assemblePerformance for local debug-signed builds."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(checkProductionSigning)
}
