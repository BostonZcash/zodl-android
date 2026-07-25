package co.electriccoin.zcash.ui.screen.ironwood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.repository.WalletRepository
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.ExternalUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val GUIDE_URL = "https://support.zodl.com/article/42-moving-your-funds-to-ironwood"

class IronwoodAnnouncementVM(
    private val navigationRouter: NavigationRouter,
    private val walletRepository: WalletRepository,
) : ViewModel() {
    val state =
        MutableStateFlow(
            IronwoodAnnouncementState(
                onGuideClick = { navigationRouter.forward(ExternalUrl(GUIDE_URL)) },
                primaryButton =
                    ButtonState(
                        text = stringRes(R.string.ironwood_announcement_primary_button),
                    ) {
                        viewModelScope.launch {
                            walletRepository.markIronwoodAnnouncementShown()
                            navigationRouter.back()
                        }
                    },
            )
        )
}
