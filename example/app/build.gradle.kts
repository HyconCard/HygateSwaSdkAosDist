plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "kr.co.beinteractive.hygateswasdktest"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.co.beinteractive.hygateswasdktest"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            /*
             * R8 을 끈다. 이 앱의 목적은 **이미 난독화된 SDK AAR** 이 그대로 동작하는지
             * 확인하는 것이다. 여기서 R8 을 또 돌리면 무엇 때문에 깨졌는지 구분이 어려워진다.
             */
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    /*
     * HygateSWA SDK. 저장소 주소는 settings.gradle.kts 에 있다.
     *
     * 이것만 넣으면 BLE·위치 권한이 SDK 매니페스트에서 자동으로 병합된다.
     * 이 앱의 AndroidManifest 에 권한을 하나도 적지 않은 이유다 — 그게 맞는지 확인하는 것도
     * 검증 항목이다.
     */
    implementation(libs.hygateswa.sdk)

    // SDK 는 kotlinx-coroutines-core 만 의존한다. Dispatchers.Main 을 쓰려면 앱이 넣어야 한다.
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
