package kr.co.beinteractive.hygateswasdktest

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.co.beinteractive.hygate.swa.SwaAdvertisement
import kr.co.beinteractive.hygate.swa.SwaBle
import kr.co.beinteractive.hygate.swa.SwaException
import kr.co.beinteractive.hygate.swa.SwaLogger
import kr.co.beinteractive.hygate.swa.SwaSession
import kr.co.beinteractive.hygate.swa.SwaScanFilter
import kr.co.beinteractive.hygate.swa.SwaSessionPolicy

/** 화면이 그리는 상태. */
data class SampleUiState(
    val bluetoothEnabled: Boolean = false,
    val scanning: Boolean = false,
    val devices: List<SwaAdvertisement> = emptyList(),
    val connectedAddress: String? = null,
    val connecting: Boolean = false,
    val authenticated: Boolean = false,
    val remainingSeconds: Int = 0,
    val mtu: Int = 0,
    val revision12: Boolean = false,
    val busy: Boolean = false,
    /** 신호 세기는 슬라이더로 받으므로 `Int?` 다. `null` 은 제한 없음이다. */
    val filterAddress: String = "",
    val filterName: String = "",
    val filterMinRssi: Int? = null,
) {
    /** 화면이 쓰는 필터. 비어 있는 칸은 조건에서 빠진다. */
    val scanFilter: SwaScanFilter
        get() = SwaScanFilter(
            address = filterAddress,
            name = filterName,
            minRssi = filterMinRssi,
        )
}

/**
 * SDK 호출을 전부 여기서 한다.
 *
 * `withSession { }` 이 아니라 [SwaBle.openSession] + 수동 [SwaSession.close] 를 쓴다.
 * 화면이 떠 있는 동안 `remainingSeconds` / `connected` / `authenticated` 를 계속 구독해야
 * 해서 세션이 블록보다 오래 살아야 하기 때문이다.
 */
class SampleViewModel(application: Application) : AndroidViewModel(application) {

    private val logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = logs.asStateFlow()

    private val state = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = state.asStateFlow()

    /** SDK 내부 로그도 같은 창에 보여준다. 비밀값은 SDK 가 길이만 넘긴다. */
    private val ble = SwaBle(
        application,
        object : SwaLogger {
            override fun debug(message: String) = append(LogEntry.Kind.Sdk, message)
            override fun error(message: String, throwable: Throwable?) =
                append(LogEntry.Kind.Sdk, message + (throwable?.let { " (${it.describe()})" } ?: ""))
        },
    )

    private var session: SwaSession? = null
    private var scanJob: Job? = null
    private var observeJob: Job? = null

    init {
        refreshBluetooth()
        append(LogEntry.Kind.Info, "SDK 세션 수명 ${SwaSessionPolicy.MAX_LIFETIME_SECONDS}초, 예약 ${SwaSessionPolicy.CONFIGURATION_RESERVE_SECONDS}초")
    }

    fun refreshBluetooth() {
        state.value = state.value.copy(bluetoothEnabled = ble.isBluetoothEnabled)
    }

    // ---------------------------------------------------------------- 필터
    //
    // 스캔은 필터 없이 돌리고 **표시할 때** SwaScanFilter.matches 로 거른다.
    // ble.scan(filter) 로 방출 자체를 막을 수도 있지만, 그러면 필터를 바꿀 때마다 스캔을
    // 다시 시작해야 해서 이미 찾은 장치가 사라진다.

    fun setFilterAddress(v: String) { state.value = state.value.copy(filterAddress = v) }

    fun setFilterName(v: String) { state.value = state.value.copy(filterName = v) }

    /** `null` 은 제한 없음. 슬라이더 하한에서 이 값이 온다. */
    fun setFilterMinRssi(v: Int?) { state.value = state.value.copy(filterMinRssi = v) }

    // ------------------------------------------------------------------ 스캔

    fun toggleScan() {
        if (state.value.scanning) {
            scanJob?.cancel()
            scanJob = null
            state.value = state.value.copy(scanning = false)
            append(LogEntry.Kind.Info, "스캔 중지")
            return
        }
        append(LogEntry.Kind.Call, "scan()")
        state.value = state.value.copy(scanning = true, devices = emptyList())
        scanJob = viewModelScope.launch {
            val found = linkedMapOf<String, SwaAdvertisement>()
            runCatching {
                ble.scan().collect { device ->
                    val isNew = !found.containsKey(device.address)
                    val gotName = found[device.address]?.installLocationName == null &&
                        device.installLocationName != null
                    found[device.address] = device
                    state.value = state.value.copy(devices = found.values.sortedByDescending { it.rssi })
                    // 교대 광고로 위치명이 뒤늦게 붙는 순간을 눈으로 확인하기 위해 찍는다.
                    if (isNew) append(LogEntry.Kind.Ok, "발견 ${device.canonicalName} ${device.rssi}dBm")
                    if (gotName) append(LogEntry.Kind.Ok, "위치명 관측 ${device.canonicalName} → ${device.installLocationName}")
                }
            }.onFailure {
                if (it !is kotlinx.coroutines.CancellationException) {
                    append(LogEntry.Kind.Fail, "scan() ${it.describe()}")
                }
            }
            state.value = state.value.copy(scanning = false)
        }
    }

    // ------------------------------------------------------------- 연결·해제

    fun connect(address: String) {
        if (state.value.connecting || session != null) return
        scanJob?.cancel(); scanJob = null
        state.value = state.value.copy(scanning = false, connecting = true)
        append(LogEntry.Kind.Call, "openSession($address)")
        viewModelScope.launch {
            runCatching { ble.openSession(address) }
                .onSuccess { opened ->
                    session = opened
                    append(LogEntry.Kind.Ok, "연결됨 MTU=${opened.mtu} 상위리비전=${opened.isRevision12Supported}")
                    state.value = state.value.copy(
                        connecting = false,
                        connectedAddress = address,
                        mtu = opened.mtu,
                        revision12 = opened.isRevision12Supported,
                    )
                    observe(opened)
                }
                .onFailure {
                    append(LogEntry.Kind.Fail, "openSession ${it.describe()}")
                    state.value = state.value.copy(connecting = false)
                }
        }
    }

    private fun observe(opened: SwaSession) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            launch { opened.remainingSeconds.collect { state.value = state.value.copy(remainingSeconds = it) } }
            launch { opened.authenticated.collect { state.value = state.value.copy(authenticated = it) } }
            launch {
                opened.connected.collect { alive ->
                    if (!alive && state.value.connectedAddress != null) {
                        append(LogEntry.Kind.Info, "connected=false (연결 종료)")
                    }
                }
            }
        }
    }

    fun disconnect() {
        append(LogEntry.Kind.Call, "close()")
        observeJob?.cancel(); observeJob = null
        session?.close()
        session = null
        state.value = state.value.copy(
            connectedAddress = null, authenticated = false,
            remainingSeconds = 0, mtu = 0, revision12 = false,
        )
        append(LogEntry.Kind.Ok, "세션 종료")
    }

    // ------------------------------------------------------------ SDK 호출들

    fun authenticate(pin: String) = call("authenticate(길이 ${pin.length})") { it.authenticate(pin) }

    fun readDeviceInfo() = call("readDeviceInfo()") { it.readDeviceInfo() }

    fun readSettings() = call("readSettings()") { it.readSettings() }

    /**
     * MQTT 계정 조회.
     *
     * 상위 리비전 펌웨어 전용이라 구 펌웨어 장치에서는 UnsupportedByFirmware 가 난다.
     * 그것도 확인해야 하는 동작이라 버튼은 항상 누를 수 있게 두고, 화면에서 미리 안내한다.
     *
     * 계정 값은 로그에 남기지 않는다. 설정 여부와 길이만 본다(SDK 정책과 동일).
     */
    fun readMqttCredentials() = call("readMqttCredentials()") { session ->
        val c = session.readMqttCredentials()
        "username ${c.username.length}자, password ${c.password.length}자"
    }

    fun writeGps(lat: Double, lon: Double) = call("writeGpsLocation($lat, $lon)") { it.writeGpsLocation(lat, lon) }
    fun clearGps() = call("clearGpsLocation()") { it.clearGpsLocation() }

    fun writeName(name: String) = call("writeInstallLocationName(\"$name\")") { it.writeInstallLocationName(name) }
    fun clearName() = call("clearInstallLocationName()") { it.clearInstallLocationName() }

    fun writeBroker(host: String) = call("writeMqttBroker(\"$host\")") { it.writeMqttBroker(host) }
    fun writePort(port: Int) = call("writeMqttPort($port)") { it.writeMqttPort(port) }
    fun writeTopic(topic: String) = call("writeMqttTopic(\"$topic\")") { it.writeMqttTopic(topic) }
    fun writeQos(qos: Int) = call("writeMqttQos($qos)") { it.writeMqttQos(qos) }
    fun writeKeepAlive(sec: Int) = call("writeMqttKeepAlive($sec)") { it.writeMqttKeepAlive(sec) }

    fun writeUsername(v: String) = call("writeMqttUsername(길이 ${v.length})") { it.writeMqttUsername(v) }
    fun clearUsername() = call("clearMqttUsername()") { it.clearMqttUsername() }
    fun writePassword(v: String) = call("writeMqttPassword(길이 ${v.length})") { it.writeMqttPassword(v) }
    fun clearPassword() = call("clearMqttPassword()") { it.clearMqttPassword() }

    /**
     * 모든 SDK 호출을 같은 방식으로 기록한다.
     *
     * 반환값이면 `✓`, 예외면 `✗` 와 **예외 타입 이름**을 남긴다. 검증 항목이
     * "어떤 예외가 오는가" 라서 타입이 메시지보다 중요하다.
     */
    private fun call(label: String, block: suspend (SwaSession) -> Any?) {
        val active = session
        if (active == null) {
            append(LogEntry.Kind.Fail, "$label — 먼저 연결하세요")
            return
        }
        if (state.value.busy) {
            append(LogEntry.Kind.Info, "$label — 이전 작업이 진행 중입니다(SDK 가 직렬화하지만 UI 에서도 막습니다)")
            return
        }
        append(LogEntry.Kind.Call, label)
        state.value = state.value.copy(busy = true)
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            runCatching { block(active) }
                .onSuccess { append(LogEntry.Kind.Ok, "$label → $it  (${System.currentTimeMillis() - started}ms)") }
                .onFailure { error ->
                    append(LogEntry.Kind.Fail, "$label  ${error.describe()}")
                    // 연결 종료는 원인 구분이 중요해 한 줄 더 남긴다.
                    (error as? SwaException.ConnectionLost)?.let {
                        append(
                            LogEntry.Kind.Info,
                            if (it.expectedExpiry) "→ 정상적인 3분 만료입니다(오류 아님). 다시 연결하세요"
                            else "→ 통신 오류로 끊겼습니다",
                        )
                    }
                }
            state.value = state.value.copy(busy = false)
        }
    }

    // -------------------------------------------------------------- 로그

    fun clearLog() {
        logs.value = emptyList()
    }

    fun logText(): String = logs.value.joinToString("\n") { it.format() }

    private fun append(kind: LogEntry.Kind, text: String) {
        val entry = LogEntry(System.currentTimeMillis(), kind, text)
        Log.i(TAG, entry.format())
        // 오래된 것부터 버려 메모리가 무한히 늘지 않게 한다.
        logs.value = (logs.value + entry).takeLast(MAX_LOG)
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        observeJob?.cancel()
        session?.close()
        session = null
    }

    private companion object {
        const val TAG = "SwaSample"
        const val MAX_LOG = 500
    }
}
