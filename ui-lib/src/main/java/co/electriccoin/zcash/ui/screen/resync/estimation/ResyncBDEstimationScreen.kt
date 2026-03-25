package co.electriccoin.zcash.ui.screen.resync.estimation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.common.compose.SecureScreen
import co.electriccoin.zcash.ui.screen.restore.estimation.RestoreBDEstimationView
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ResyncBDEstimationScreen(args: ResyncBDEstimationArgs) {
    val vm = koinViewModel<ResyncBDEstimationVM> { parametersOf(args) }
    val state by vm.state.collectAsStateWithLifecycle()
    SecureScreen()
    BackHandler(enabled = state != null) { state?.onBack() }
    state?.let { RestoreBDEstimationView(it) }
}

@Serializable
data class ResyncBDEstimationArgs(
    val uuid: String,
    val blockHeight: Long
)
