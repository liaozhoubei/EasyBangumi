plugins {
    `kotlin-dsl`
}

repositories {
    maven { url =uri("https://maven.aliyun.com/repository/google") }
    maven { url =uri("https://maven.aliyun.com/repository/gradle-plugin") }
    maven { url =uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
    google()

}