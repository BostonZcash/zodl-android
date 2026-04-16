package co.electriccoin.zcash.ui.screen.restore.tor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.common.LceRenderer
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RestoreTorScreen(args: RestoreTorArgs) {
    val vm = koinViewModel<RestoreTorVM> { parametersOf(args) }
    val state by vm.state.collectAsStateWithLifecycle()
    LceRenderer(state) { RestoreTorView(it) }
}

@Serializable
data class RestoreTorArgs(
    val seed: String,
    val blockHeight: Long
)
