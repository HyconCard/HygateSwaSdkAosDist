package kr.co.beinteractive.hygateswasdktest

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 로그 한 줄. */
data class LogEntry(
    val timeMillis: Long,
    val kind: Kind,
    val text: String,
) {
    enum class Kind { Call, Ok, Fail, Info, Sdk }

    fun format(): String = "${TIME.format(Date(timeMillis))} ${kind.mark()} $text"

    private fun Kind.mark(): String = when (this) {
        Kind.Call -> "→"
        Kind.Ok -> "✓"
        Kind.Fail -> "✗"
        Kind.Info -> "·"
        Kind.Sdk -> "sdk"
    }

    private companion object {
        val TIME = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    }
}

/**
 * 예외를 로그에 적을 문자열로 만든다.
 *
 * **타입 이름을 그대로 보여주는 것이 핵심이다.** 검증 항목이 "`SessionExpiring` 이 오는가",
 * "`expectedExpiry` 가 true 인가" 같은 식이라 메시지만으로는 확인할 수 없다.
 */
fun Throwable.describe(): String {
    val name = this::class.java.name
        .substringAfterLast('.')
        .replace('$', '.')
    val detail = message?.takeIf { it.isNotBlank() }
    return if (detail != null) "$name: $detail" else name
}
