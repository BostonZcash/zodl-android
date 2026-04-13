package co.electriccoin.zcash.ui.screen.connectkeystone.date

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.VersionInfo
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.fixture.WalletFixture
import co.electriccoin.zcash.ui.screen.common.BirthdayPickerState
import co.electriccoin.zcash.ui.screen.connectkeystone.estimation.KeystoneEstimationArgs
import co.electriccoin.zcash.ui.screen.connectkeystone.height.KeystoneHeightArgs
import co.electriccoin.zcash.ui.screen.heightinfo.HeightInfoArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import kotlin.time.toKotlinInstant

class KeystoneDateVM(
    private val args: KeystoneDateArgs,
    private val navigationRouter: NavigationRouter,
    private val application: Application,
) : ViewModel() {
    private val selection = MutableStateFlow(WalletFixture.SAPLING_ACTIVATION_YEAR_MONTH)
    private val isEstimating = MutableStateFlow(false)

    val state: StateFlow<BirthdayPickerState> =
        combine(selection, isEstimating) { yearMonth, estimating ->
            createState(yearMonth, estimating)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = createState(selection.value, false),
        )

    private fun createState(yearMonth: YearMonth, isLoading: Boolean) =
        BirthdayPickerState(
            title = null,
            subtitle = stringRes(R.string.keystone_first_transaction_subtitle),
            message = stringRes(R.string.keystone_first_transaction_message),
            logo = co.electriccoin.zcash.ui.design.R.drawable.image_keystone,
            selection = yearMonth,
            primaryButton =
                ButtonState(
                    text = stringRes(R.string.keystone_first_transaction_next),
                    isLoading = isLoading,
                    isEnabled = !isLoading,
                    onClick = { onEstimateClick(yearMonth) },
                ),
            secondaryButton =
                ButtonState(
                    text = stringRes(R.string.keystone_first_transaction_enter_height),
                    onClick = ::onEnterBlockHeightClick,
                ),
            dialogButton =
                IconButtonState(
                    icon = R.drawable.ic_help,
                    onClick = ::onInfoClick,
                ),
            onBack = ::onBack,
            onYearMonthChange = ::onYearMonthChange,
        )

    private fun onEstimateClick(yearMonth: YearMonth) {
        if (isEstimating.value) return
        viewModelScope.launch {
            isEstimating.value = true
            try {
                val instant =
                    yearMonth
                        .atDay(1)
                        .atStartOfDay()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toKotlinInstant()
                val bday =
                    SdkSynchronizer.estimateBirthdayHeight(
                        context = application,
                        date = instant,
                        network = VersionInfo.NETWORK,
                    )
                navigationRouter.forward(
                    KeystoneEstimationArgs(
                        ur = args.ur,
                        blockHeight = bday.value,
                    )
                )
            } finally {
                isEstimating.value = false
            }
        }
    }

    private fun onEnterBlockHeightClick() = navigationRouter.forward(KeystoneHeightArgs(args.ur))

    private fun onBack() = navigationRouter.back()

    private fun onInfoClick() = navigationRouter.forward(HeightInfoArgs)

    private fun onYearMonthChange(yearMonth: YearMonth) = selection.update { yearMonth }
}
