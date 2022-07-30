pluginManagement {
    repositories {
        maven { url =uri("https://maven.aliyun.com/repository/google") }
        maven { url =uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url =uri("https://maven.aliyun.com/repository/public") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url =uri("https://maven.aliyun.com/repository/google" )}
        maven { url =uri("https://maven.aliyun.com/repository/gradle-plugin" )}
        maven { url =uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()

    }
}
rootProject.name = "EasyBangumi"
include (":app")
