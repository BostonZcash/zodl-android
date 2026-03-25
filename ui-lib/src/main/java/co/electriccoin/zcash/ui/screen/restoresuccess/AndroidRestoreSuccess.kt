package co.electriccoin.zcash.ui.screen.restoresuccess

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.electriccoin.zcash.di.koinActivityViewModel
import kotlinx.serialization.Serializable

@Composable
fun WrapRestoreSuccess() {
    val viewModel = koinActivityViewModel<RestoreSuccessViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    RestoreSuccessView(state)
}

@Serializable
data object WrapRestoreSuccessArgs
