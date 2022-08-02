plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("kotlin-kapt")
    id("com.google.devtools.ksp") version "1.6.21-1.0.5"
    kotlin("plugin.serialization") version "1.6.21"

}

android {
    namespace = "com.heyanle.easybangumi"
    compileSdk = AndroidConfig.compileSdk
    buildToolsVersion = AndroidConfig.buildToolsVersion

    defaultConfig {
        applicationId = "com.heyanle.easybangumi"
        minSdk = AndroidConfig.defaultConfig.minSdk
        targetSdk = AndroidConfig.defaultConfig.targetSdk
        versionCode = AndroidConfig.defaultConfig.versionCode
        versionName = AndroidConfig.defaultConfig.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }




    buildTypes {
        debug{
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
//            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),  "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.leanback:leanback:1.0.0")
    implementation( "androidx.leanback:leanback-preference:1.0.0")
//    implementation( "com.google.android.exoplayer:exoplayer:2.6.1")
    implementation( "com.google.android.exoplayer:extension-leanback:2.17.1")
    implementation ("com.google.android.exoplayer:exoplayer-core:2.17.1")
    implementation ("com.google.android.exoplayer:exoplayer-dash:2.17.1")
    implementation ("com.google.android.exoplayer:exoplayer-ui:2.17.1")
    Dependencies.project.forEach { implementation(project(it)) }
    Dependencies.implementation.forEach { implementation(it) }
    Dependencies.ksp.forEach { ksp(it) }
    Dependencies.kapt.forEach { kapt(it) }
    Dependencies.debugImplementation.forEach { debugImplementation(it) }
    Dependencies.testImplementation.forEach { testImplementation(it) }
    Dependencies.androidTestImplementation.forEach { androidTestImplementation(it) }

}
