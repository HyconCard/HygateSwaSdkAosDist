# HygateSWA Android SDK

SWA(Stop Work Authority) 장치를 Android 앱에서 설정하기 위한 BLE SDK 배포 저장소입니다.

주식회사 비인터랙티브가 공급한 SWA 장치를 제어하는 앱을 개발하기 위한 것입니다.

## 설치

**1) `settings.gradle.kts`의 `repositories` 블록에 한 줄 추가**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://hyconcard.github.io/HygateSwaSdkAosDist/maven") }
    }
}
```

**2) SDK를 쓸 모듈에 의존성 추가**

```kotlin
dependencies {
    implementation("kr.co.beinteractive.hygate:hygateswa-sdk:1.0.0")
}
```

버전을 올릴 때는 숫자만 바꾸면 됩니다. 사용 가능한 버전은
[Releases](../../releases)에서 확인하세요.

## 사용법

전체 매뉴얼은 각 릴리스에 첨부된 `hygateswa-sdk-<버전>.zip` 안의 `README.md`에 있습니다.
설치, 권한, 시작하기, API 레퍼런스, 오류 처리를 담고 있습니다.

### 요구 사항

| 항목 | 값 |
|---|---|
| minSdk | 24 이상 (Android 7.0) |
| Java / Kotlin | JVM target 11 이상 |
| 외부 의존성 | `kotlinx-coroutines-core` 하나 |

## 이 저장소에 있는 것

- 이 안내문
- Releases에 올라가는 배포 패키지(`hygateswa-sdk-<버전>.zip`)
- `maven/` — Gradle이 의존성을 받아가는 정적 maven 저장소

소스 코드는 포함되지 않습니다.

## 문의

연동 중 막히는 부분이 있으면 아래 정보를 함께 알려주시면 확인이 빠릅니다.

- SDK 버전
- 장치 펌웨어 버전
- Android 버전과 기기 모델
- 발생한 예외 메시지

주식회사 비인터랙티브

## 라이선스

독점 라이선스입니다. 배포 패키지 안의 `LICENSE.txt`를 참고하세요.
재배포와 역공학은 허용되지 않습니다.
