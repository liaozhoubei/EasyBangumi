import com.heyanle.buildsrc.*
import org.gradle.kotlin.dsl.project

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.heyanle.easybangumi"
    compileSdk = Android.compileSdk

    defaultConfig {
        applicationId = "com.heyanle.easybangumi"
        minSdk = Android.minSdk
        targetSdk = Android.targetSdk
        versionCode = Android.versionCode
        versionName = Android.versionName
        // Android.versionName在 buildSrc 中配置
        flavorDimensions.add(Android.versionName)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas".toString()
            }
        }

        buildConfigField(
            "String",
            Config.APP_CENTER_SECRET,
            "\"${Config.getPrivateValue(Config.APP_CENTER_SECRET)}\""
        )
    }

    packagingOptions {
        resources.excludes.add("META-INF/beans.xml")
    }

    buildTypes {
        release {
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = false
                isOptimizeCode = true
                proguardFiles("proguard-rules.pro")
            }
        }
    }

    productFlavors{
        create("phone") {}
        // 添加构建变体，国内 tv 系统无法区分手机还是电视应用
        create("tv") {
            applicationIdSuffix = ".tv"
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
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }

}


dependencies {

    val leanback_version = "1.2.0-alpha02"

    implementation("androidx.leanback:leanback:$leanback_version")
    implementation("androidx.leanback:leanback-preference:$leanback_version")
//    implementation( "com.google.android.exoplayer:exoplayer-ui:${Version.exoplayer}")
    val media3_version = "1.1.0"
    implementation("androidx.media3:media3-ui-leanback:1.1.0")
    // For media playback using ExoPlayer
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    // For building media playback UIs
    implementation("androidx.media3:media3-ui:$media3_version")
    // For building media playback UIs for Android TV using the Jetpack Leanback library
    implementation("androidx.media3:media3-ui-leanback:$media3_version")
    // For HLS playback support with ExoPlayer
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")
    glide()
    okkv2()
    okhttp3()
    androidXBasic()
    leakcanary()
    paging()
    pagingCompose()
    junit()
    compose()
    accompanist()
    navigationCompose()
    coil()
    coilGif()
    exoplayer()
    exoplayerHls()
    exoplayerRtmp()
    media()
    easyPlayer()
    room()
    roomPaging()
    appCenter()
    gson()
    jsoup()
    androidXWebkit()
    commonsText()
    cling()
    implementation(project(":easy-crasher"))
    implementation(project(":source-core"))
    implementation(project(":source-api"))
    implementation(project(":easy-dlna"))
    implementation(project(":easy-i18n"))
}