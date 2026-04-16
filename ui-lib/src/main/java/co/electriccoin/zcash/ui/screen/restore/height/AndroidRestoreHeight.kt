package co.electriccoin.zcash.ui.screen.restore.height

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.common.compose.SecureScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AndroidRestoreHeight(args: RestoreHeight) {
    val vm = koinViewModel<RestoreHeightVM> { parametersOf(args) }
    val state by vm.state.collectAsStateWithLifecycle()
    SecureScreen()
    BackHandler { state.onBack() }
    co.electriccoin.zcash.ui.screen.common
        .BlockHeightView(state)
}

@Serializable
data class RestoreHeight(
    val seed: String
)
