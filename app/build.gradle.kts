plugins {
    id("com.android.application")
}

android {
    namespace = "com.antisleep.keepscreen"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antisleep.keepscreen"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.3"
    }

    buildTypes {
        release {
            // 防息屏辅助工具，不做混淆；使用 debug 签名便于直接安装
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 零第三方依赖：全部使用 Android 框架 API，降低构建与兼容风险
}
