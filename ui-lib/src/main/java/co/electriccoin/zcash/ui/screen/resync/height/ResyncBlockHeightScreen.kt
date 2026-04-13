package co.electriccoin.zcash.ui.screen.resync.height

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.di.koinActivityViewModel
import co.electriccoin.zcash.ui.screen.common.BlockHeightView
import kotlinx.serialization.Serializable

@Composable
fun ResyncBlockHeightScreen() {
    val viewModel = koinActivityViewModel<ResyncBlockHeightVM>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    BlockHeightView(state)
}

@Serializable
data object ResyncBlockHeightArgs
