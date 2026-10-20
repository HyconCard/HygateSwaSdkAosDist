# HygateSWA SDK 예제 앱

SDK 를 직접 써 보는 앱이다. **버튼 하나가 SDK 함수 하나에 대응하고**, 호출 결과와 예외를
그대로 화면 로그에 보여준다. SDK 를 붙이기 전에 장치가 어떻게 응답하는지 먼저 볼 수 있다.

## SDK 참조

로컬 소스나 동봉 AAR 을 쓰지 않는다. 배포된 것을 그대로 받아 쓴다.

```kotlin
// settings.gradle.kts
maven { url = uri("https://hyconcard.github.io/HygateSwaSdkAosDist/maven") }

// gradle/libs.versions.toml
hygateswaSdk = "0.3.3"
```

## 쓰는 법

1. Android Studio 에서 이 폴더를 **Open** (New Project 아님)
2. **Gradle Sync** — 이때 SDK 를 내려받는다. 네트워크가 필요하다
3. **실기기에 Run.** 에뮬레이터는 BLE 가 없어 동작하지 않는다
4. 권한을 전부 허용한다. **위치는 "정확한 위치"** 여야 스캔 결과가 온다
5. 스캔 → 연결 → 버튼을 눌러 본다
6. 하단 로그의 **복사** 버튼으로 결과를 가져갈 수 있다 (`adb logcat -s SwaSample` 에도 남는다)

## 화면 구성

| 영역 | 내용 |
|---|---|
| 상태 바 | BT·연결·인증·Rev1.2·MTU·남은 시간. 켜짐은 초록 점, 남은 시간은 60초 이하 주황 · 25초 이하 빨강(그 아래로는 SDK 가 저장을 거부한다) |
| 스캔 | 장치 목록. 교대 광고로 위치명이 뒤늦게 붙는 것을 볼 수 있다 |
| 인증 | 비밀번호 입력. 형식 검증을 `SwaPin.isValid` 로 미리 보여준다 |
| 조회 | 장치정보 / 설정 / MQTT계정. 계정은 인증 후에만 읽힌다 |
| 설치 설정 | 좌표·위치명 쓰기와 삭제. 위치명은 바이트 수와 위반 사유를 실시간 표시 |
| MQTT | 브로커·포트·토픽·QoS·KeepAlive·계정 |
| 로그 | 호출 → 반환값 또는 **예외 타입** |

로그가 반환값과 예외를 나눠 보여주는 것은 **SDK 가 그렇게 구분하기 때문이다** — 통신이
정상적으로 끝난 처리 결과는 반환값으로, 요청 자체가 성립하지 않은 경우는 예외로 온다.
그래서 메시지가 아니라 `SwaException.SessionExpiring` 같은 **타입 이름**을 그대로 찍는다.

## 버튼이 부르는 SDK 함수

SDK 호출은 **전부 `SampleViewModel.kt` 한 파일에** 있다. 버튼을 눌러 보고 그 파일에서
같은 이름을 찾으면 된다.

함수별 시그니처·파라미터·예외·예제는 **SDK README 4절**에 있다.

| 화면 버튼 | SDK 함수 |
|---|---|
| 스캔 시작/중지 | `SwaBle.scan()` |
| 장치 행의 `연결` | `SwaBle.openSession(address)` |
| 연결 해제 | `SwaSession.close()` |
| 인증 | `authenticate(pin)` |
| 장치정보 | `readDeviceInfo()` |
| 설정 | `readSettings()` |
| MQTT계정 | `readMqttCredentials()` |
| 좌표 쓰기 / 삭제 | `writeGpsLocation(lat, lon)` / `clearGpsLocation()` |
| 위치명 쓰기 / 삭제 | `writeInstallLocationName(name)` / `clearInstallLocationName()` |
| 브로커 쓰기 | `writeMqttBroker(host)` |
| 포트 | `writeMqttPort(port)` |
| 토픽 쓰기 | `writeMqttTopic(topic)` |
| QoS | `writeMqttQos(qos)` |
| KeepAlive | `writeMqttKeepAlive(seconds)` |
| 계정 쓰기 / 삭제 | `writeMqttUsername(v)` / `clearMqttUsername()` |
| 비번 쓰기 / 삭제 | `writeMqttPassword(v)` / `clearMqttPassword()` |

상태 바가 보여 주는 값은 `SwaSession` 의 속성을 그대로 구독한 것이다 —
`connected` / `authenticated` / `remainingSeconds` / `mtu` / `isRevision12Supported`.

### 객체 두 개의 수명

| 객체 | 얻는 법 | 수명 |
|---|---|---|
| `SwaBle` | `SwaBle(context, logger)` | 앱에 하나 두고 재사용한다. 상태가 없다 |
| `SwaSession` | `ble.openSession(address)` | **연결 1건당 하나.** 끊으면 버리고 다시 받는다 |

세션을 얻는 방법이 두 가지다.

```kotlin
// 1. withSession — 블록이 끝나면 자동으로 닫는다. 대부분 이쪽이 안전하다
ble.withSession(address) { session ->
    session.authenticate(pin)
    session.writeMqttPort(1883)
}

// 2. openSession + close — 세션이 블록보다 오래 살아야 할 때
val session = ble.openSession(address)
// ... remainingSeconds 등을 화면에서 계속 구독
session.close()
```

이 앱은 **2번**을 쓴다. 화면이 떠 있는 동안 남은 시간과 연결 상태를 계속 구독해야 해서
세션이 한 블록보다 오래 살아야 하기 때문이다. 그럴 필요가 없다면 **1번을 쓰면 된다** —
닫는 것을 잊을 수 없다.

## 안전

실제 장치에 쓰는 앱이다.

- **MQTT 설정을 바꾸면 현장 장치가 서버에 못 붙는다.** MQTT 로는 복구할 수 없어 현장에서
  BLE 로 다시 설정해야 한다
- 비밀번호 값은 로그에 남기지 않는다. 길이만 찍는다

## 권한

이 앱은 매니페스트에 권한을 하나도 선언하지 않는다. **아래는 전부 SDK 매니페스트에서 자동으로
병합된 것이다.** 앱이 매니페스트를 손댈 필요가 없다.

| 권한 | 적용 범위 | 런타임 요청 |
|---|---|---|
| `BLUETOOTH` | Android 11 이하 (`maxSdkVersion="30"`) | 불필요 (설치 시 부여) |
| `BLUETOOTH_ADMIN` | Android 11 이하 (`maxSdkVersion="30"`) | 불필요 (설치 시 부여) |
| `BLUETOOTH_SCAN` | Android 12 이상 | **필요** |
| `BLUETOOTH_CONNECT` | Android 12 이상 | **필요** |
| `ACCESS_FINE_LOCATION` | 전 버전 | **필요** |
| `ACCESS_COARSE_LOCATION` | 전 버전 | 불필요 — 정확한 위치를 허용하면 함께 부여된다 |

### 런타임 요청은 앱이 한다

`SwaPermissions.required()` 가 실행 중인 OS 버전에 맞는 목록을 돌려준다.

```kotlin
launcher.launch(SwaPermissions.required())
// Android 12 이상 → ACCESS_FINE_LOCATION, BLUETOOTH_SCAN, BLUETOOTH_CONNECT
// Android 11 이하 → ACCESS_FINE_LOCATION
```

**위치는 "정확한 위치" 로 허용받아야 한다.** "대략적인 위치" 만 허용하면 스캔 결과가 오지
않고 `SwaException.PermissionDenied` 가 발생한다.

병합 결과는 `app/build/intermediates/merged_manifest/debug/AndroidManifest.xml` 에서 볼 수 있다.
거기에 위 목록 외에 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 이 보이면 AndroidX
(`androidx.core`, `androidx.emoji2`)가 넣은 것이고 이 SDK 와 무관하다.
