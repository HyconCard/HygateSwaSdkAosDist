# HygateSWA Android SDK

SWA(Stop Work Authority) 장치를 Android 앱에서 설정하기 위한 BLE SDK 배포 저장소입니다.

주식회사 비인터랙티브가 공급한 SWA 장치를 제어하는 앱을 개발하기 위한 것입니다.

## 설치

**1) `settings.gradle.kts`의 `repositories` 블록에 한 줄 추가**

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)   // 그대로 두세요
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://hyconcard.github.io/HygateSwaSdkAosDist/maven") }   // <-- 추가
    }
}
```

**2) SDK를 쓸 모듈에 의존성 추가**

```kotlin
dependencies {
    implementation("kr.co.beinteractive.hygate:hygateswa-sdk:1.0.0")
}
```

**버전은 직접 적어 주세요.** `latest.release` 나 `0.3.+` 같은 동적 버전은 권하지 않습니다 —
공급사가 배포하는 순간 고객 빌드의 SDK가 바뀝니다. 0.x 구간에서는 API 가 바뀌므로 특히 그렇습니다.

최신 버전은 한 줄로 확인할 수 있습니다.

```bash
curl -s https://hyconcard.github.io/HygateSwaSdkAosDist/maven/kr/co/beinteractive/hygate/hygateswa-sdk/maven-metadata.xml | grep release
```

[Releases](../../releases)에서 버전별 변경 내용을 함께 볼 수 있습니다.
올리기 전에 **깨지는 변경이 있는지 먼저 확인하세요.**

## 이 SDK로 할 수 있는 일

기능 하나가 함수 하나입니다. 파라미터·제약·예외는 전체 매뉴얼에 있습니다.

| 기능 | 함수 |
|---|---|
| 주변 SWA 장치 검색 | `SwaBle.scan()` |
| 연결 | `SwaBle.openSession(address)` |
| 비밀번호 인증 | `authenticate(pin)` |
| 장치 정보 조회 | `readDeviceInfo()` |
| 현재 설정 조회 | `readSettings()` |
| MQTT 계정 조회 | `readMqttCredentials()` |
| 설치 좌표 | `writeGpsLocation(lat, lon)` / `clearGpsLocation()` |
| 설치 위치명 | `writeInstallLocationName(name)` / `clearInstallLocationName()` |
| MQTT 브로커·포트·토픽 | `writeMqttBroker()` / `writeMqttPort()` / `writeMqttTopic()` |
| MQTT QoS·Keep Alive | `writeMqttQos()` / `writeMqttKeepAlive()` |
| MQTT 계정 | `writeMqttUsername()` / `writeMqttPassword()` + `clear*` |

쓰기 함수는 값을 보낸 뒤 **장치의 확정 결과를 기다리고 다시 읽어 확인한 다음** 반환합니다.
성공·실패는 반환값(`SwaConfigResult`)에 담겨 오므로, 결과를 알아내기 위한 콜백이나 알림
구독을 따로 만들 필요가 없습니다.

## 요구 사항

| 항목 | 값 |
|---|---|
| minSdk | 24 이상 (Android 7.0) |
| Java / Kotlin | JVM target 11 이상 |
| 따라붙는 의존성 | `kotlinx-coroutines-core`, `kotlin-stdlib` 둘뿐입니다 |

BLE·위치 권한은 SDK 매니페스트에서 자동으로 병합됩니다. 앱 매니페스트에 적을 것은 없고,
런타임 권한 요청만 앱이 합니다(`SwaPermissions.required()`).

## 전체 매뉴얼

각 릴리스에 첨부된 `hygateswa-sdk-<버전>.zip` 안의 `README.md`에 있습니다.
설치, 권한, 시작하기, 기능과 호출 함수, 데이터 모델, 꼭 알아야 할 동작, 오류 처리를 담고 있습니다.

## 예제 앱

`example/` 폴더에 **동작하는 예제 프로젝트**가 있습니다. 위와 똑같이 원격 좌표로 SDK를 받는
구성이고, 버튼 하나가 SDK 함수 하나에 대응합니다.

```bash
git clone https://github.com/HyconCard/HygateSwaSdkAosDist.git
# example/ 폴더를 Android Studio 에서 Open → 실기기에 Run
```

BLE가 필요하므로 에뮬레이터가 아닌 **실기기**에서 실행해야 합니다. 자세한 설명은
`example/README.md`에 있습니다.

| 파일 | 내용 |
|---|---|
| `SampleViewModel.kt` | **SDK 호출 전부.** 스캔·연결·인증·조회·설정 변경·예외 처리 |
| `SampleScreen.kt` | 화면. 버튼과 입력 검증 표시 |
| `SampleLog.kt` | 호출 결과와 예외 타입 로그 |
| `MainActivity.kt` | 권한 요청(`SwaPermissions.required()`) |

## 이 저장소에 있는 것

- 이 안내문
- `example/` — 예제 프로젝트 소스
- Releases에 올라가는 배포 패키지(`hygateswa-sdk-<버전>.zip`)
- `maven/` — Gradle이 의존성을 받아가는 정적 maven 저장소

SDK 소스 코드는 포함되지 않습니다.

## 문의

연동 중 막히는 부분이 있으면 아래 정보를 함께 알려주시면 확인이 빠릅니다.

- SDK 버전
- 장치 펌웨어 버전 (`readDeviceInfo().firmwareVersion`)
- Android 버전과 기기 모델
- 발생한 예외 메시지

주식회사 비인터랙티브

## 라이선스

SDK는 주식회사 비인터랙티브의 자산이며, **별도의 서면 계약에 따라 사용을 허가받은 경우에만**
사용할 수 있습니다. 전문은 배포 패키지 안의 `LICENSE.txt` 에 있습니다.
