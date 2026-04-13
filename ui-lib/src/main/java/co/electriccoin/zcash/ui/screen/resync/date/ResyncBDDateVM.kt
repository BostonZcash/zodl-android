package co.electriccoin.zcash.ui.screen.resync.date

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.VersionInfo
import co.electriccoin.zcash.ui.common.usecase.GetResyncDataFromHeightUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToEstimateBlockHeightUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.common.BirthdayPickerState
import co.electriccoin.zcash.ui.screen.heightinfo.HeightInfoArgs
import co.electriccoin.zcash.ui.screen.resync.estimation.ResyncBDEstimationArgs
import co.electriccoin.zcash.ui.screen.resync.height.ResyncBlockHeightArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import kotlin.time.toKotlinInstant

class ResyncBDDateVM(
    private val args: ResyncBDDateArgs,
    private val navigationRouter: NavigationRouter,
    private val application: Application,
    private val navigateToEstimateBlockHeight: NavigateToEstimateBlockHeightUseCase,
    private val getResyncDataFromHeight: GetResyncDataFromHeightUseCase
) : ViewModel() {
    private val selection = MutableStateFlow<YearMonth?>(null)

    init {
        viewModelScope.launch {
            val data = getResyncDataFromHeight(BlockHeight.new(args.initialBlockHeight))
            selection.update { data }
        }
    }

    val state: StateFlow<BirthdayPickerState?> =
        selection
            .filterNotNull()
            .map {
                createState(it)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = null
            )

    private fun createState(selection: YearMonth) =
        BirthdayPickerState(
            title = stringRes(R.string.resync_title),
            subtitle = stringRes(R.string.resync_bd_date_subtitle),
            message = stringRes(R.string.resync_bd_date_message),
            logo = null,
            primaryButton =
                ButtonState(stringRes(R.string.restore_bd_date_next), onClick = {
                    onEstimateClick(selection)
                }),
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
            selection = selection
        )

    private fun onEstimateClick(yearMonth: YearMonth) {
        viewModelScope.launch {
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
                    network = VersionInfo.NETWORK
                )
            navigationRouter.forward(ResyncBDEstimationArgs(uuid = args.uuid, blockHeight = bday.value))
        }
    }

    private fun onBack() {
        viewModelScope.launch {
            navigateToEstimateBlockHeight.onSelectionCancelled(args)
        }
    }

    private fun onEnterBlockHeightClick() = navigationRouter.forward(ResyncBlockHeightArgs)

    private fun onInfoClick() = navigationRouter.forward(HeightInfoArgs)

    private fun onYearMonthChange(yearMonth: YearMonth) = selection.update { yearMonth }
}
