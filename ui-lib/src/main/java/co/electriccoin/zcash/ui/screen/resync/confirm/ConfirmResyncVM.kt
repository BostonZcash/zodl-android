package co.electriccoin.zcash.ui.screen.resync.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.extension.launchSingle
import co.electriccoin.zcash.ui.common.provider.PersistableWalletProvider
import co.electriccoin.zcash.ui.common.usecase.ConfirmResyncUseCase
import co.electriccoin.zcash.ui.common.usecase.GetResyncDataFromHeightUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToEstimateBlockHeightUseCase
import co.electriccoin.zcash.ui.common.usecase.ShowErrorUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.StringResourceColor
import co.electriccoin.zcash.ui.design.util.StyledStringStyle
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.design.util.stringResByNumber
import co.electriccoin.zcash.ui.design.util.styledStringResource
import co.electriccoin.zcash.ui.design.util.withStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

class ConfirmResyncVM(
    persistableWalletProvider: PersistableWalletProvider,
    private val navigationRouter: NavigationRouter,
    private val navigateToEstimateBlockHeight: NavigateToEstimateBlockHeightUseCase,
    private val confirmResync: ConfirmResyncUseCase,
    private val getResyncDataFromHeight: GetResyncDataFromHeightUseCase,
    private val showError: ShowErrorUseCase
) : ViewModel() {
    private val blockHeight = MutableStateFlow<BlockHeight?>(null)

    private var changeJob: Job? = null
    private var confirmJob: Job? = null

    init {
        viewModelScope.launch {
            val wallet = persistableWalletProvider.requirePersistableWallet()
            blockHeight.update { wallet.birthday }
        }
    }

    private val yearMonthFlow =
        blockHeight
            .filterNotNull()
            .map { getResyncDataFromHeight(it) }

    val state: StateFlow<ConfirmResyncState?> =
        combine(
            blockHeight.filterNotNull(),
            yearMonthFlow
        ) { height, yearMonth ->
            createState(height, yearMonth)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = null
        )

    private fun createState(height: BlockHeight, yearMonth: YearMonth): ConfirmResyncState =
        ConfirmResyncState(
            title = stringRes(R.string.resync_title),
            subtitle = stringRes(R.string.confirm_resync_title),
            message = stringRes(R.string.confirm_resync_subtitle),
            onBack = ::onBack,
            confirm =
                ButtonState(
                    stringRes(R.string.confirm_resync_confirm),
                    onClick = {
                        onConfirmClick(height)
                    }
                ),
            change =
                ButtonState(
                    stringRes(R.string.confirm_resync_change),
                    onClick = {
                        onChangeClick(height)
                    }
                ),
            changeInfo =
                styledStringResource(
                    resource = R.string.confirm_resync_info,
                    style =
                        StyledStringStyle(
                            color = StringResourceColor.TERTIARY,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        ),
                    stringRes(yearMonth).withStyle(
                        StyledStringStyle(
                            color = StringResourceColor.PRIMARY,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    ),
                    stringResByNumber(height.value, 0).withStyle(
                        style =
                            StyledStringStyle(
                                color = StringResourceColor.TERTIARY
                            )
                    )
                )
        )

    private fun onBack() = navigationRouter.back()

    private fun onChangeClick(height: BlockHeight) {
        viewModelScope.launchSingle(::changeJob) {
            navigateToEstimateBlockHeight(height)
        }
    }

    private fun onConfirmClick(height: BlockHeight) {
        viewModelScope.launchSingle(::confirmJob) {
            try {
                confirmResync(height)
            } catch (_: Exception) {
                showError()
            }
        }
    }
}
