package co.electriccoin.zcash.ui.screen.connectkeystone.estimation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.ui.screen.common.EstimatedBlockHeightView
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun KeystoneFirstTransactionEstimationScreen(args: KeystoneEstimationArgs) {
    val vm = koinViewModel<KeystoneEstimationVM> { parametersOf(args) }
    val state by vm.state.collectAsStateWithLifecycle()
    val errorState by vm.errorState.collectAsStateWithLifecycle()
    BackHandler(enabled = state != null) { state?.onBack?.invoke() }
    state?.let { EstimatedBlockHeightView(state = it, errorState = errorState) }
}
