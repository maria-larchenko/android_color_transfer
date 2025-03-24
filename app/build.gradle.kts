plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.catman.mkl"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.catman.mkl"
        minSdk = 28
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.android)
    implementation(libs.litert.support.api)
    implementation(libs.androidx.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

//    implementation(libs.multik.core)
//    implementation(libs.multik.core.jvm)
//    implementation(libs.multik.default)
//    implementation("org.jetbrains.kotlinx:multik-core:0.2.3")
//    implementation("org.jetbrains.kotlinx:multik-default:0.2.3")
//    implementation("org.jetbrains.kotlinx:multik-core-jvm:0.2.3")
//    implementation("org.jetbrains.kotlinx:multik-core:0.2.0")

    implementation("org.ejml:ejml-core:0.43")
    implementation("org.ejml:ejml-ddense:0.43")
    implementation("org.ejml:ejml-fdense:0.43")
    implementation("org.ejml:ejml-simple:0.43")

//    implementation(libs.ndarray)
//    implementation(libs.litert.api)
//    implementation(libs.litert)
//    implementation `com.google.ai.edge.litert:litert:1.0.1`
}