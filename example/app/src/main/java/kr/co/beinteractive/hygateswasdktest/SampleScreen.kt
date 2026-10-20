package kr.co.beinteractive.hygateswasdktest

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.beinteractive.hygate.swa.SwaAdvertisement
import kr.co.beinteractive.hygate.swa.protocol.SwaInstallLocationName
import kr.co.beinteractive.hygate.swa.protocol.SwaMqttLimits
import kr.co.beinteractive.hygate.swa.protocol.SwaNameViolation
import kr.co.beinteractive.hygate.swa.protocol.SwaPin

@Composable
fun SampleScreen(viewModel: SampleViewModel) {
    val state by viewModel.uiState.collectAsStateCompat()
    val logs by viewModel.log.collectAsStateCompat()

    /*
     * Scaffold 의 기본 인셋 패딩을 끈다. 그러면 위아래 어두운 영역(상태 줄·로그 창)이
     * 시스템 바 뒤까지 그려지고, 아이콘이 그 어두운 배경 위에 올라와 보이게 된다.
     * 대신 각 영역이 자기 인셋만큼 안쪽 여백을 직접 준다.
     */
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            /*
             * 로그 창이 화면을 다 먹지 않도록 상한을 화면 높이에 맞춰 둔다.
             *
             * 상한은 드래그할 때만이 아니라 **그릴 때마다** 적용한다. 키보드가 올라오거나
             * 화면을 회전하면 쓸 수 있는 높이가 줄어드는데, 끌어 둔 값만 그대로 두면
             * 위쪽 조작부가 0 이 되어 아무것도 안 보인다.
             */
            val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            /*
             * Scaffold 가 더 이상 인셋을 빼 주지 않으므로 시스템 바가 먹는 높이를 직접 뺀다.
             * 거기에 조작부 최소 높이까지 확보하고 남는 만큼만 로그 창에 허용한다.
             * 이렇게 해야 어떤 화면에서도 위쪽 버튼이 완전히 사라지지 않는다.
             */
            val maxLogHeight = (maxHeight - statusTop - navBottom - MIN_CONTENT_HEIGHT)
                .coerceAtLeast(MIN_LOG_HEIGHT)
            var logHeight by remember { mutableStateOf(260.dp) }
            val effectiveLogHeight = logHeight.coerceIn(MIN_LOG_HEIGHT, maxLogHeight)

            Column(Modifier.fillMaxSize()) {
                StatusBar(state)
                HorizontalDivider()
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ScanSection(state, viewModel)
                    if (state.connectedAddress != null) {
                        AuthSection(viewModel, state)
                        ReadSection(state, viewModel)
                        InstallSection(viewModel)
                        MqttSection(viewModel)
                    }
                }
                LogPane(
                    logs = logs,
                    vm = viewModel,
                    height = effectiveLogHeight,
                    onDrag = { deltaDp ->
                        // 위로 끌면 delta 가 음수다. 로그 창은 그만큼 커져야 하므로 뺀다.
                        logHeight = (effectiveLogHeight - deltaDp)
                            .coerceIn(MIN_LOG_HEIGHT, maxLogHeight)
                    },
                )
            }
        }
    }
}

private val MIN_LOG_HEIGHT = 72.dp

/** 로그 창을 아무리 키워도 조작부에 남겨 둘 최소 높이. */
private val MIN_CONTENT_HEIGHT = 140.dp

// ------------------------------------------------------------------ 상태 바

/*
 * 상태 표시.
 *
 * 한 줄짜리 긴 문자열이면 좁은 화면에서 잘리고, O/X 만으로는 한눈에 안 들어온다.
 * 항목마다 작은 칩을 두고 FlowRow 로 감싸 폭에 맞춰 줄바꿈되게 한다.
 * 켜짐·꺼짐은 글자가 아니라 **색과 점**으로 구분한다.
 */
@Composable
private fun StatusBar(state: SampleUiState) {
    FlowRow(
        Modifier.fillMaxWidth()
            .background(Color(0xFF1E1E22))
            // 배경을 먼저 깔고 인셋 패딩을 준다. 그래야 시스템 바 뒤도 어둡게 칠해진다.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Flag("BT", state.bluetoothEnabled)
        Flag("연결", state.connectedAddress != null)
        Flag("인증", state.authenticated)
        Flag("Rev1.2", state.revision12)
        ValueChip("MTU", if (state.mtu > 0) state.mtu.toString() else "—")
        SessionChip(state.remainingSeconds, connected = state.connectedAddress != null)
        if (state.busy) {
            Chip(Color(0xFF3A2E10)) {
                Text("작업중", color = Color(0xFFD29922), fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** 켜짐·꺼짐 칩. 점 색으로 구분해 글자를 읽지 않아도 보인다. */
@Composable
private fun Flag(label: String, on: Boolean) {
    Chip(if (on) Color(0xFF14301C) else Color(0xFF2A2A32)) {
        Box(
            Modifier
                .size(7.dp)
                .background(if (on) Color(0xFF7EE787) else Color(0xFF70707A), CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (on) Color(0xFF7EE787) else Color(0xFFA5A5AE),
            fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
        )
    }
}

/** 숫자 값 칩. 라벨은 흐리게, 값은 밝게 둬서 값이 먼저 읽히게 한다. */
@Composable
private fun ValueChip(label: String, value: String) {
    Chip(Color(0xFF2A2A32)) {
        Text(label, color = Color(0xFFA5A5AE), fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.width(5.dp))
        Text(
            value, color = Color(0xFFE6E6EA), fontSize = 13.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1,
        )
    }
}

/**
 * 남은 세션 시간. 가장 중요한 정보라 크게 두고 색으로 단계를 알린다.
 *
 * 25초 이하면 SDK 가 새 설정 저장을 거부하므로(SessionExpiring) 그 경계를 빨강으로 띄운다.
 */
@Composable
private fun SessionChip(seconds: Int, connected: Boolean) {
    val (bg, fg, text) = when {
        !connected -> Triple(Color(0xFF2A2A32), Color(0xFFA5A5AE), "—")
        seconds <= 0 -> Triple(Color(0xFF3A1614), Color(0xFFFF7B72), "만료")
        seconds <= 25 -> Triple(Color(0xFF3A1614), Color(0xFFFF7B72), seconds.clock())
        seconds <= 60 -> Triple(Color(0xFF3A2E10), Color(0xFFD29922), seconds.clock())
        else -> Triple(Color(0xFF2A2A32), Color(0xFFE6E6EA), seconds.clock())
    }
    Chip(bg) {
        Text("남은시간", color = Color(0xFFA5A5AE), fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.width(5.dp))
        Text(
            text, color = fg, fontSize = 14.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1,
        )
    }
}

@Composable
private fun Chip(background: Color, content: @Composable () -> Unit) {
    Row(
        Modifier
            .background(background, RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

private fun Int.clock() = "%d:%02d".format(this / 60, this % 60)

// -------------------------------------------------------------------- 스캔

@Composable
private fun ScanSection(state: SampleUiState, vm: SampleViewModel) {
    Section("스캔") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { vm.toggleScan() }, enabled = state.connectedAddress == null) {
                Text(if (state.scanning) "스캔 중지" else "스캔 시작")
            }
            if (state.connectedAddress != null) {
                OutlinedButton(onClick = { vm.disconnect() }) { Text("연결 해제") }
            }
        }
        if (state.connectedAddress == null) {
            ScanFilterFields(state, vm)
            Hint("장치가 안 보이면: 전원·거리·위치 권한(정확한 위치)을 확인하세요.")
            val visible = state.devices.filter { state.scanFilter.matches(it) }
            if (state.devices.isNotEmpty()) {
                Text(
                    if (state.scanFilter.isEmpty) "장치 ${state.devices.size}대"
                    else "${visible.size} / ${state.devices.size}대 (필터 적용)",
                    fontSize = 11.sp, color = Color.Gray,
                )
            }
            visible.forEach { DeviceRow(it) { vm.connect(it.address) } }
        } else {
            Text(
                "연결됨: ${state.connectedAddress}", fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 스캔 필터 입력. SDK 가 주소·이름·신호 세기를 각각 받으므로 칸도 각각 둔다.
 *
 * 스캔을 다시 시작하지 않고 **표시할 때** `SwaScanFilter.matches` 로 거른다. 그래야 입력을
 * 지웠을 때 이미 찾은 장치가 그대로 다시 보인다.
 */
@Composable
private fun ScanFilterFields(state: SampleUiState, vm: SampleViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Field("MAC 일부", state.filterAddress, vm::setFilterAddress, Modifier.weight(1f))
        Field("이름 일부", state.filterName, vm::setFilterName, Modifier.weight(1f))
    }
    Field("최소 RSSI (dBm, 비우면 전체)", state.filterMinRssi, vm::setFilterMinRssi)
    Constraint(
        rule = "MAC·이름은 부분 일치(대소문자·구분자 무시) · RSSI 는 그 값 이상만. 예: -70",
        // 입력 도중의 "-" 는 아직 숫자가 아니므로 오류로 보지 않는다.
        problem = "RSSI 가 숫자가 아닙니다".takeIf {
            state.filterMinRssi.isNotBlank() && state.filterMinRssi != "-" &&
                state.filterMinRssi.toIntOrNull() == null
        },
    )
}

@Composable
private fun DeviceRow(device: SwaAdvertisement, onConnect: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // weight(1f) 로 남는 폭만 쓰게 하고, 넘치면 말줄임한다.
            // 그러지 않으면 긴 위치명이 "연결" 버튼을 화면 밖으로 밀어낸다.
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    device.installLocationName ?: device.canonicalName,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${device.canonicalName} · ${device.address} · ${device.rssi}dBm" +
                        if (device.serviceRequired) " · 점검필요" else "",
                    fontSize = 12.sp, color = Color.Gray,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (device.installLocationName == null) {
                    Text("위치명 아직 못 봄(교대 광고 대기)", fontSize = 11.sp, color = Color.Gray)
                }
            }
            Button(onClick = onConnect) { Text("연결") }
        }
    }
}

// -------------------------------------------------------------------- 인증

@Composable
private fun AuthSection(vm: SampleViewModel, state: SampleUiState) {
    var pin by remember { mutableStateOf(SwaPin.DEFAULT_PIN) }
    Section("인증") {
        // PIN 은 길이를 입력 단계에서 막는다. 10자를 넘기면 받지 않는다.
        Field("비밀번호", pin, { if (it.length <= SwaPin.MAX_LENGTH) pin = it })
        Constraint(
            rule = "${SwaPin.MIN_LENGTH}~${SwaPin.MAX_LENGTH}자, 공백 없는 ASCII",
            state = "${pin.length}/${SwaPin.MAX_LENGTH}자",
            problem = if (SwaPin.isValid(pin)) null else "형식이 맞지 않습니다",
            atLimit = pin.length >= SwaPin.MAX_LENGTH,
        )
        Button(onClick = { vm.authenticate(pin) }, enabled = !state.busy) { Text("인증") }
        Hint("설정 변경은 인증 후에만 됩니다. 인증은 연결 단위라 재연결하면 다시 해야 합니다.")
    }
}

// -------------------------------------------------------------------- 조회

@Composable
private fun ReadSection(state: SampleUiState, vm: SampleViewModel) {
    Section("조회") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { vm.readDeviceInfo() }) { Text("장치정보") }
            OutlinedButton(onClick = { vm.readSettings() }) { Text("설정") }
            OutlinedButton(onClick = { vm.readMqttCredentials() }) { Text("MQTT계정") }
        }
        Hint("장치정보·설정은 인증이 필요 없습니다. MQTT 계정은 인증 후에만 읽힙니다.")
        /*
         * 버튼을 막지 않고 미리 알려 준다. 구 펌웨어에서 UnsupportedByFirmware 가 오는 것도
         * 확인해야 하는 동작이라, 가려 버리면 그 경로를 볼 수 없다.
         */
        if (!state.revision12) {
            Warning(
                "이 장치는 구 펌웨어입니다(상태 바 Rev1.2 꺼짐). MQTT 계정 조회는 상위 리비전 " +
                    "전용이라 UnsupportedByFirmware 가 발생합니다. 펌웨어 버전은 장치정보로 확인하세요.",
            )
        }
    }
}

// --------------------------------------------------------------- 설치 설정

@Composable
private fun InstallSection(vm: SampleViewModel) {
    var lat by remember { mutableStateOf("37.5665") }
    var lon by remember { mutableStateOf("126.9780") }
    var name by remember { mutableStateOf("현장1동") }

    Section("설치 설정") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("위도", lat, { lat = it }, Modifier.weight(1f))
            Field("경도", lon, { lon = it }, Modifier.weight(1f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = {
                val la = lat.toDoubleOrNull(); val lo = lon.toDoubleOrNull()
                if (la == null || lo == null) vm.writeGps(Double.NaN, 0.0) else vm.writeGps(la, lo)
            }) { Text("좌표 쓰기") }
            OutlinedButton(onClick = { vm.clearGps() }) { Text("좌표 삭제") }
        }

        Spacer(Modifier.height(4.dp))
        /*
         * 길이는 **입력 단계에서 막는다.** 한도를 넘는 글자는 아예 들어오지 않는다.
         * truncateToMaxBytes 가 글자 경계에서 자르므로 한글이 반토막 나지 않는다.
         *
         * 길이 외의 규칙(+++, 앞뒤 +, 제어문자)은 막지 않고 문구로만 알린다.
         * 타이핑 도중 일시적으로 위반 상태가 되기 때문이다 — 예를 들어 "A+B" 를 치는
         * 중간에는 "A+" 라서 끝에 + 가 온다. 그때마다 입력을 거부하면 칠 수가 없다.
         */
        Field("설치 위치명", name, { input ->
            name = SwaInstallLocationName.truncateToMaxBytes(input)
        })
        val bytes = SwaInstallLocationName.byteCountOf(name)
        Constraint(
            rule = "UTF-8 ${SwaInstallLocationName.MAX_BYTES}바이트 이하(한글 9자) · " +
                "제어문자와 +++, 앞뒤 + 불가",
            state = "$bytes/${SwaInstallLocationName.MAX_BYTES}바이트",
            problem = SwaInstallLocationName.violationOf(name)?.describe(),
            atLimit = bytes >= SwaInstallLocationName.MAX_BYTES,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { vm.writeName(name) }) { Text("위치명 쓰기") }
            OutlinedButton(onClick = { vm.clearName() }) { Text("위치명 삭제") }
        }
        Hint("MTU 32 미만인 기기에서 긴 이름을 쓰면 어떤 예외가 오는지 확인해 보세요.")
    }
}

/** 위반 사유를 사람이 읽을 문구로. */
private fun SwaNameViolation.describe(): String = when (this) {
    SwaNameViolation.TooLong -> "${SwaInstallLocationName.MAX_BYTES}바이트를 넘었습니다"
    SwaNameViolation.ControlCharacter -> "제어 문자는 쓸 수 없습니다"
    SwaNameViolation.TriplePlus -> "+++ 는 쓸 수 없습니다"
    SwaNameViolation.LeadingPlus -> "+ 로 시작할 수 없습니다"
    SwaNameViolation.TrailingPlus -> "+ 로 끝날 수 없습니다"
}

// ----------------------------------------------------------------- MQTT

@Composable
private fun MqttSection(vm: SampleViewModel) {
    var broker by remember { mutableStateOf("10.0.0.2") }
    var port by remember { mutableStateOf("1883") }
    var topic by remember { mutableStateOf("swa/test") }
    var qos by remember { mutableStateOf("1") }
    var keepAlive by remember { mutableStateOf("60") }   // 장치 기본값
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Section("MQTT 접속 설정") {
        Warning("현장 장치에 쓰면 서버 접속이 끊길 수 있습니다. 복구하려면 현장에서 다시 설정해야 합니다.")

        Field("브로커", broker, { if (it.length <= SwaMqttLimits.BROKER_MAX_LENGTH) broker = it })
        Constraint(
            rule = "ASCII ${SwaMqttLimits.BROKER_MAX_LENGTH}자 이하 · 영문자·숫자·점·하이픈 · " +
                "각 구간이 영숫자로 시작하고 끝나야 함",
            state = "${broker.length}/${SwaMqttLimits.BROKER_MAX_LENGTH}자",
            problem = if (SwaMqttLimits.isValidBroker(broker)) null else "규칙에 맞지 않습니다",
            atLimit = broker.length >= SwaMqttLimits.BROKER_MAX_LENGTH,
        )
        Button(onClick = { vm.writeBroker(broker) }) { Text("브로커 쓰기") }

        /*
         * 한 줄에 세 칸을 넣으면 좁은 화면에서 칸이 100dp 아래로 줄어 라벨이 잘린다.
         * 두 칸씩 나눠 둔다. 칸이 좁아져도 라벨이 읽히는 선을 지키는 것이 낫다.
         */
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("포트", port, { port = it }, Modifier.weight(1f))
            Field("QoS", qos, { qos = it }, Modifier.weight(1f))
        }
        Field("Keep Alive (초)", keepAlive, { keepAlive = it })
        Constraint(
            rule = "포트 ${SwaMqttLimits.PORT_MIN}~${SwaMqttLimits.PORT_MAX} · QoS 0·1·2 · " +
                "KeepAlive ${SwaMqttLimits.KEEP_ALIVE_MIN_SECONDS}~${SwaMqttLimits.KEEP_ALIVE_MAX_SECONDS}초",
            problem = listOfNotNull(
                "포트".takeIf { port.toIntOrNull()?.let(SwaMqttLimits::isValidPort) != true },
                "QoS".takeIf { qos.toIntOrNull()?.let(SwaMqttLimits::isValidQos) != true },
                "KeepAlive".takeIf { keepAlive.toIntOrNull()?.let(SwaMqttLimits::isValidKeepAlive) != true },
            ).takeIf { it.isNotEmpty() }?.joinToString(", ", postfix = " 값이 범위를 벗어났습니다"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { vm.writePort(port.toIntOrNull() ?: -1) }) { Text("포트") }
            OutlinedButton(onClick = { vm.writeQos(qos.toIntOrNull() ?: -1) }) { Text("QoS") }
            OutlinedButton(onClick = { vm.writeKeepAlive(keepAlive.toIntOrNull() ?: -1) }) { Text("KeepAlive") }
        }

        Field("토픽", topic, { if (it.length <= SwaMqttLimits.TOPIC_MAX_LENGTH) topic = it })
        Constraint(
            rule = "printable ASCII ${SwaMqttLimits.TOPIC_MAX_LENGTH}자 이하 · " +
                "따옴표·역슬래시와 앞뒤 +, +++ 불가",
            state = "${topic.length}/${SwaMqttLimits.TOPIC_MAX_LENGTH}자",
            problem = if (SwaMqttLimits.isValidTopic(topic)) null else "규칙에 맞지 않습니다",
            atLimit = topic.length >= SwaMqttLimits.TOPIC_MAX_LENGTH,
        )
        Button(onClick = { vm.writeTopic(topic) }) { Text("토픽 쓰기") }

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("계정", user, { if (it.length <= SwaMqttLimits.CREDENTIAL_MAX_LENGTH) user = it }, Modifier.weight(1f))
            Field("비밀번호", pass, { if (it.length <= SwaMqttLimits.CREDENTIAL_MAX_LENGTH) pass = it }, Modifier.weight(1f))
        }
        Constraint(
            rule = "ASCII ${SwaMqttLimits.CREDENTIAL_MAX_LENGTH}자 이하 · 공백·제어문자 불가 · " +
                "지울 때는 빈 값이 아니라 삭제 버튼",
            state = "계정 ${user.length} · 비번 ${pass.length} / ${SwaMqttLimits.CREDENTIAL_MAX_LENGTH}자",
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { vm.writeUsername(user) }) { Text("계정 쓰기") }
            OutlinedButton(onClick = { vm.clearUsername() }) { Text("계정 삭제") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { vm.writePassword(pass) }) { Text("비번 쓰기") }
            OutlinedButton(onClick = { vm.clearPassword() }) { Text("비번 삭제") }
        }
        Hint("값을 지울 때는 빈 문자열이 아니라 삭제 버튼을 씁니다. 빈 값은 Validation 예외입니다.")
    }
}

// -------------------------------------------------------------------- 로그

@Composable
private fun LogPane(
    logs: List<LogEntry>,
    vm: SampleViewModel,
    height: Dp,
    onDrag: (Dp) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
    }
    /*
     * 내비게이션 바 높이만큼 더 크게 그리고, 그만큼을 맨 아래 Spacer 로 비워 둔다.
     * 이렇게 하면 어두운 배경이 바 뒤까지 이어져 아이콘이 보이고,
     * 사용자가 끌어 맞춘 로그 영역 높이는 줄어들지 않는다.
     */
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxWidth().height(height + navBottom).background(Color(0xFF14141A))) {
        /*
         * 크기 조절 손잡이. 로그가 길어지면 넓히고, 조작부를 봐야 하면 줄인다.
         * 터치 영역을 22dp 로 둔 이유는 선만 그리면 손가락으로 잡기 어렵기 때문이다.
         */
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(Color(0xFF24242C))
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        onDrag(with(density) { delta.toDp() })
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .background(Color(0xFF6E7681), RoundedCornerShape(2.dp)),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("로그 ${logs.size}", color = Color(0xFF9B9B96), fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { clipboard.setText(AnnotatedString(vm.logText())) }) { Text("복사") }
            TextButton(onClick = { vm.clearLog() }) { Text("지우기") }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            items(logs) { entry ->
                Text(
                    entry.format(),
                    color = when (entry.kind) {
                        LogEntry.Kind.Ok -> Color(0xFF7EE787)
                        LogEntry.Kind.Fail -> Color(0xFFFF7B72)
                        LogEntry.Kind.Call -> Color(0xFF79C0FF)
                        LogEntry.Kind.Sdk -> Color(0xFF6E7681)
                        LogEntry.Kind.Info -> Color(0xFFD29922)
                    },
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                )
            }
        }
        // 내비게이션 바가 가리는 만큼 비워 둔다. 배경은 이미 그 뒤까지 칠해져 있다.
        Spacer(Modifier.height(navBottom))
    }
}

// ------------------------------------------------------------------ 조각

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        modifier = modifier.then(if (modifier == Modifier) Modifier.fillMaxWidth() else Modifier),
    )
}

/**
 * 입력란 아래에 제약을 상시 표시한다.
 *
 * @param rule 지켜야 하는 규칙. 입력 전에도 보인다
 * @param state 현재 상태(바이트 수 등). 없으면 생략
 * @param problem 위반 사유. 없으면 `null`
 * @param atLimit 한도에 닿아 더 입력되지 않는 상태
 */
@Composable
private fun Constraint(
    rule: String,
    state: String? = null,
    problem: String? = null,
    atLimit: Boolean = false,
) {
    Text(rule, fontSize = 11.sp, color = Color.Gray)
    val detail = when {
        problem != null -> "✗ $problem"
        atLimit -> "● $state — 한도에 도달해 더 입력되지 않습니다"
        state != null -> "✓ $state"
        else -> null
    }
    if (detail != null) {
        Text(
            detail,
            fontSize = 11.sp,
            color = when {
                problem != null -> Color(0xFFC62828)
                atLimit -> Color(0xFFB26A00)
                else -> Color(0xFF2E7D32)
            },
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 11.sp, color = Color.Gray)
}

@Composable
private fun Warning(text: String) {
    Text(text, fontSize = 11.sp, color = Color(0xFFB26A00), fontWeight = FontWeight.Medium)
}

