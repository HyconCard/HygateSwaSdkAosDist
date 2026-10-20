package kr.co.beinteractive.hygateswasdktest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import kr.co.beinteractive.hygate.swa.SwaPermissions

class MainActivity : ComponentActivity() {

    /**
     * 런타임 권한은 앱이 직접 요청한다. SDK 는 목록만 준다.
     *
     * **"정확한 위치" 로 허용해야 스캔 결과가 온다.** 대략적인 위치만 허용하면
     * Android 12 이상에서도 `PermissionDenied` 가 발생한다.
     */
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
         * targetSdk 35 부터 edge-to-edge 가 강제된다. 화면이 시스템 바 뒤까지 그려지므로
         * 바 아이콘(시계·배터리) 색을 배경에 맞춰 주지 않으면 흰 배경에 흰 아이콘이 되어
         * 아무것도 안 보인다.
         *
         * 이 앱은 위아래 끝이 둘 다 어두운 영역이다(상태 줄 / 로그 창). 그래서 양쪽 바를
         * "어두운 배경" 으로 선언해 아이콘을 흰색으로 띄운다. 화면 테마(시스템 다크 모드)와
         * 무관하게 고정해야 한다 — 배경색이 우리 쪽에서 고정이기 때문이다.
         */
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        permissionLauncher.launch(SwaPermissions.required())
        setContent {
            MaterialTheme {
                val model: SampleViewModel = viewModel()
                SampleScreen(model)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 설정 앱에서 블루투스를 켜고 돌아오는 경로를 반영한다.
        (viewModelStoreOwnerViewModel())?.refreshBluetooth()
    }

    private fun viewModelStoreOwnerViewModel(): SampleViewModel? =
        runCatching {
            androidx.lifecycle.ViewModelProvider(this)[SampleViewModel::class.java]
        }.getOrNull()
}

/** Compose 버전 차이를 흡수하는 얇은 래퍼. */
@Composable
fun <T> StateFlow<T>.collectAsStateCompat(): State<T> = collectAsState()
