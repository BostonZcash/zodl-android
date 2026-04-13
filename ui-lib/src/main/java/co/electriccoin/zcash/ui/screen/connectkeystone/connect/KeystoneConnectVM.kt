package co.electriccoin.zcash.ui.screen.connectkeystone.connect

import androidx.lifecycle.ViewModel
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.screen.connectkeystone.explainer.KeystoneExplainerScreenArgs
import co.electriccoin.zcash.ui.screen.scankeystone.ScanKeystoneSignInRequest
import co.electriccoin.zcash.ui.screen.selectkeystoneaccount.SelectKeystoneAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeystoneConnectVM(
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    val state: StateFlow<KeystoneConnectState> =
        MutableStateFlow(
            KeystoneConnectState(
                onBackClick = ::onBack,
                onContinueClick = ::onContinue,
                onViewKeystoneTutorialClicked = ::onViewKeystoneTutorial,
            )
        ).asStateFlow()

    private fun onBack() = navigationRouter.back()

    private fun onContinue() = navigationRouter.forward(ScanKeystoneSignInRequest)

    private fun onViewKeystoneTutorial() = navigationRouter.forward(KeystoneExplainerScreenArgs)
}
