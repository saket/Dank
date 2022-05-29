@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.agp)
    alias(libs.plugins.kotlin)
}

android {
    namespace = "com.imashnake.dank"
    compileSdk = 32

    defaultConfig {
        applicationId = "com.imashnake.dank"
        minSdk = 26
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.get()
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.coreKtx)
    implementation(libs.androidx.lifecycleRuntimeKtx)
    implementation(libs.androidx.activityCompose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.toolingPreview)
    implementation(libs.compose.material3)

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espressoCore)
    androidTestImplementation(libs.compose.test.ui.test.junit4)
    debugImplementation(libs.compose.test.ui.tooling)
    debugImplementation(libs.compose.test.ui.test.manifest)
}
